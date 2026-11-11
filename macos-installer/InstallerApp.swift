import AppKit
import SwiftUI

struct StockLauncher: Decodable {
    let package: String
    let enabled: Bool?
}

struct ConnectedTV: Decodable {
    let model: String
    let android: String
    let transport: String
    let suggested: String?
    let stock: [StockLauncher]
}

struct BackendReply: Decodable {
    let ok: Bool
    let error: String?
    let log: [String]?
    let device: ConnectedTV?
    let endpoint: String?
}

struct InstallerError: LocalizedError {
    let errorDescription: String?
    init(_ message: String) { errorDescription = message }
}

enum InstallerFiles {
    static let apk = "TV-Home-Apple-Style.apk"
    static let guide = "TV-Home-USB-Instruction.txt"

    static func resource(_ name: String) throws -> URL {
        guard let url = Bundle.main.resourceURL?.appendingPathComponent(name),
              FileManager.default.fileExists(atPath: url.path) else {
            throw InstallerError("В приложении отсутствует файл " + name + ".")
        }
        return url
    }
}

enum Backend {
    static func run(_ arguments: [String], pairingCode: String? = nil) throws -> BackendReply {
        let helpers = Bundle.main.bundleURL.appendingPathComponent("Contents/Helpers")
        #if arch(arm64)
        let executable = "TVHomeBackend-arm64"
        #else
        let executable = "TVHomeBackend-x64"
        #endif
        let output = try process(helpers.appendingPathComponent(executable),
                                 [helpers.appendingPathComponent("adb").path] + arguments,
                                 input: pairingCode)
        let replies = output.split(whereSeparator: \.isNewline)
        if let line = replies.last, let reply = try? JSONDecoder().decode(BackendReply.self, from: Data(line.utf8)) {
            return reply
        }
        throw InstallerError(output.isEmpty ? "Служебная программа не вернула результат." : output)
    }

    static func verifyBundle() throws {
        let adb = Bundle.main.bundleURL.appendingPathComponent("Contents/Helpers/adb")
        let version = try process(adb, ["version"])
        guard version.contains("Android Debug Bridge"), version.contains("37.0.1"),
              try run(["endpoint", "192.0.2.1"]).endpoint == "192.0.2.1:5555",
              (try Data(contentsOf: InstallerFiles.resource(InstallerFiles.apk))).count > 20_000_000,
              !(try String(contentsOf: InstallerFiles.resource(InstallerFiles.guide), encoding: .utf8)).isEmpty else {
            throw InstallerError("Проверка встроенных файлов не прошла.")
        }
    }

    private static func process(_ executable: URL, _ arguments: [String], input: String? = nil) throws -> String {
        let task = Process()
        task.executableURL = executable
        task.arguments = arguments
        let output = Pipe()
        task.standardOutput = output
        task.standardError = output
        let stdin = Pipe()
        if input != nil { task.standardInput = stdin }
        try task.run()
        if let input {
            try stdin.fileHandleForWriting.write(contentsOf: Data((input + "\n").utf8))
            try stdin.fileHandleForWriting.close()
        }
        let data = output.fileHandleForReading.readDataToEndOfFile()
        task.waitUntilExit()
        return String(decoding: data, as: UTF8.self).trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

@MainActor
final class InstallerModel: ObservableObject {
    @Published var address = "" {
        didSet {
            device = nil
            selection = ""
        }
    }
    @Published var device: ConnectedTV?
    @Published var selection = ""
    @Published var disableAfterInstall = false
    @Published var busy = false
    @Published var status = "Подключите Mac и Android TV к одной сети."

    var stock: StockLauncher? { device?.stock.first(where: { $0.package == selection }) }

    func connect() {
        perform(["connect", address], pending: "Подключаюсь к устройству…", finished: "Устройство подключено.")
    }

    func install() {
        guard let device else { return }
        do {
            let apk = try InstallerFiles.resource(InstallerFiles.apk)
            perform(["install", address, device.transport, apk.path, disableAfterInstall ? selection : ""],
                    pending: "Передаю APK и проверяю запуск…", finished: "Установка завершена.")
        } catch {
            status = error.localizedDescription
        }
    }

    func switchHome() {
        guard let device, let stock, let enabled = stock.enabled else { return }
        perform(["switch", address, device.transport, stock.package, enabled ? "off" : "on"],
                pending: "Проверяю и переключаю главный экран…", finished: "Главный экран переключён.")
    }

    func pair(address: String, code: String) {
        perform(["pair", address], pairingCode: code, pending: "Выполняю сопряжение…",
                finished: "Сопряжение выполнено. Введите адрес и порт подключения с главного экрана беспроводной отладки.")
    }

    func saveToUSB() {
        let dialog = NSOpenPanel()
        dialog.canChooseDirectories = true
        dialog.canChooseFiles = false
        dialog.prompt = "Сохранить сюда"
        dialog.message = "Выберите флешку или папку для APK и инструкции."
        guard dialog.runModal() == .OK, let folder = dialog.url else { return }
        do {
            let names = [InstallerFiles.apk, InstallerFiles.guide]
            if names.contains(where: { FileManager.default.fileExists(atPath: folder.appendingPathComponent($0).path) }) {
                let alert = NSAlert()
                alert.messageText = "Заменить существующие файлы установщика?"
                alert.addButton(withTitle: "Заменить")
                alert.addButton(withTitle: "Отмена")
                guard alert.runModal() == .alertFirstButtonReturn else { return }
            }
            for name in names {
                try Data(contentsOf: InstallerFiles.resource(name))
                    .write(to: folder.appendingPathComponent(name), options: .atomic)
            }
            status = "APK и инструкция сохранены в папку «\(folder.lastPathComponent)»."
        } catch {
            status = error.localizedDescription
        }
    }

    private func perform(_ arguments: [String], pairingCode: String? = nil, pending: String, finished: String) {
        guard !busy else { return }
        busy = true
        status = pending
        Task {
            defer { busy = false }
            do {
                let reply = try await Task.detached {
                    try Backend.run(arguments, pairingCode: pairingCode)
                }.value
                var lines = reply.log ?? []
                lines.append(reply.ok ? finished : reply.error ?? "Операция завершилась с ошибкой.")
                status = lines.joined(separator: "\n")
                if let current = reply.device {
                    if !current.stock.contains(where: { $0.package == selection }) {
                        selection = current.suggested ?? ""
                    }
                    device = current
                } else if !reply.ok {
                    device = nil
                    selection = ""
                }
            } catch {
                status = error.localizedDescription
                device = nil
                selection = ""
            }
        }
    }
}

struct InstallerView: View {
    @ObservedObject var model: InstallerModel
    @State private var showsPairing = false
    @State private var showsGuide = false
    @State private var pairingAddress = ""
    @State private var pairingCode = ""
    @State private var guideText = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack(spacing: 12) {
                Image(nsImage: NSApp.applicationIconImage)
                    .resizable().frame(width: 48, height: 48).accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 4) {
                    Text("TV Home Installer").font(.title2.bold())
                    Text("Android TV и Google TV").foregroundStyle(.secondary)
                }
            }
            HStack(spacing: 8) {
                TextField("IP устройства или IP:порт", text: $model.address)
                    .textFieldStyle(.roundedBorder)
                    .accessibilityLabel("IP устройства или IP и порт")
                    .onSubmit { if !model.address.isEmpty { model.connect() } }
                Button("Подключить", action: model.connect)
                    .disabled(model.address.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .disabled(model.busy)
            GroupBox {
                VStack(alignment: .leading, spacing: 12) {
                    if let device = model.device {
                        Text("\(device.model) · Android \(device.android)").font(.headline)
                        if !device.stock.isEmpty {
                            Picker("Штатный экран", selection: $model.selection) {
                                Text("Выберите пакет").tag("")
                                ForEach(device.stock, id: \.package) { stock in
                                    Text(stock.package + (stock.enabled == false ? " · отключён" : "")).tag(stock.package)
                                }
                            }
                        } else {
                            Text("Штатный экран не определён. APK можно установить.").foregroundStyle(.secondary)
                        }
                    } else {
                        Text("Устройство ещё не подключено").foregroundStyle(.secondary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(8)
            }
            .disabled(model.busy)
            Toggle("После установки выключить штатный экран", isOn: $model.disableAfterInstall)
                .disabled(model.busy || model.device == nil)
            HStack(spacing: 12) {
                Button("Установить лаунчер", action: model.install)
                    .buttonStyle(.borderedProminent)
                    .disabled(model.device == nil || (model.disableAfterInstall && model.stock?.enabled == nil))
                Button(model.stock?.enabled == false ? "Вернуть штатный экран" : "Выключить штатный экран",
                       action: model.switchHome)
                    .disabled(model.stock?.enabled == nil)
                if model.busy { ProgressView().controlSize(.small) }
            }
            .disabled(model.busy)
            ScrollView {
                Text(model.status)
                    .textSelection(.enabled)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(10)
            }
            .frame(minHeight: 70)
            .background(Color(nsColor: .textBackgroundColor), in: RoundedRectangle(cornerRadius: 8))
            HStack {
                Button("APK для флешки", action: model.saveToUSB)
                Button("Инструкция") {
                    do {
                        guideText = try String(contentsOf: InstallerFiles.resource(InstallerFiles.guide), encoding: .utf8)
                        showsGuide = true
                    } catch {
                        model.status = error.localizedDescription
                    }
                }
                Spacer()
                Button("Сопряжение…") {
                    pairingAddress = ""
                    pairingCode = ""
                    showsPairing = true
                }
            }
            .disabled(model.busy)
        }
        .padding(24)
        .frame(minWidth: 610, minHeight: 460)
        .sheet(isPresented: $showsGuide) {
            VStack(alignment: .leading, spacing: 12) {
                ScrollView {
                    Text(guideText).textSelection(.enabled).frame(maxWidth: .infinity, alignment: .leading)
                }
                Button("Закрыть") { showsGuide = false }.keyboardShortcut(.cancelAction)
            }
            .padding(20).frame(width: 600, height: 430)
        }
        .sheet(isPresented: $showsPairing) {
            VStack(alignment: .leading, spacing: 12) {
                Text("Сопряжение ADB").font(.headline)
                Text("На Android откройте «Беспроводная отладка → Сопряжение по коду».")
                TextField("IP:порт из окна сопряжения", text: $pairingAddress)
                SecureField("Шестизначный код", text: $pairingCode)
                HStack {
                    Button("Отмена") { showsPairing = false }.keyboardShortcut(.cancelAction)
                    Spacer()
                    Button("Сопрячь") {
                        model.pair(address: pairingAddress, code: pairingCode)
                        pairingCode = ""
                        showsPairing = false
                    }
                    .keyboardShortcut(.defaultAction)
                    .disabled(!pairingAddress.contains(":") || pairingCode.utf8.count != 6 ||
                              !pairingCode.utf8.allSatisfy { (48...57).contains($0) })
                }
            }
            .textFieldStyle(.roundedBorder).padding(20).frame(width: 460)
        }
    }
}

@MainActor
final class InstallerDelegate: NSObject, NSApplicationDelegate {
    var canQuit: () -> Bool = { true }

    func applicationShouldTerminate(_ sender: NSApplication) -> NSApplication.TerminateReply {
        canQuit() ? .terminateNow : .terminateCancel
    }
}

@main
struct InstallerApp: App {
    @NSApplicationDelegateAdaptor(InstallerDelegate.self) private var delegate
    @StateObject private var model = InstallerModel()

    var body: some Scene {
        WindowGroup("TV Home Installer") {
            InstallerView(model: model)
                .onAppear {
                    delegate.canQuit = { !model.busy }
                    verifyIfRequested()
                }
        }
    }

    private func verifyIfRequested() {
        let args = CommandLine.arguments
        guard let marker = args.firstIndex(of: "--verify-bundle"), args.indices.contains(marker + 1) else { return }
        do {
            try Backend.verifyBundle()
            try Data("OK\n".utf8).write(to: URL(fileURLWithPath: args[marker + 1]), options: .atomic)
        } catch {
            try? Data(error.localizedDescription.utf8).write(to: URL(fileURLWithPath: args[marker + 1]), options: .atomic)
        }
        NSApp.terminate(nil)
    }
}
