import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    init() {
        AppModuleKt.initializeKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
