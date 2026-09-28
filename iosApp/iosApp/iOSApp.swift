import Shared
import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            // Compose lays out around the safe area itself.
            ComposeView().ignoresSafeArea()
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
