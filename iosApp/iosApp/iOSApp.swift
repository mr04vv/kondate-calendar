import FirebaseCore
import GoogleSignIn
import Shared
import SwiftUI

@main
struct iOSApp: App {
    init() {
        // GoogleService-Info.plist stays out of git; without it the app only shows a setup notice.
        if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
        }
    }

    var body: some Scene {
        WindowGroup {
            // Compose lays out around the safe area itself.
            ComposeView()
                .ignoresSafeArea()
                .onOpenURL { GIDSignIn.sharedInstance.handle($0) }
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(signIn: signInWithGoogle)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// Runs GoogleSignIn for the Kotlin side, which calls back with the tokens, nothing when cancelled, or an error message.
private func signInWithGoogle(callback: @escaping (String?, String?, String?) -> KotlinUnit) {
    guard let clientID = FirebaseApp.app()?.options.clientID else {
        _ = callback(nil, nil, "GoogleService-Info.plist に CLIENT_ID がありません")
        return
    }
    let root = UIApplication.shared.connectedScenes
        .compactMap { ($0 as? UIWindowScene)?.keyWindow?.rootViewController }
        .first
    guard let root else {
        _ = callback(nil, nil, "ログイン画面を表示できませんでした")
        return
    }
    GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
    GIDSignIn.sharedInstance.signIn(withPresenting: root) { result, error in
        if let error = error as? GIDSignInError, error.code == .canceled {
            _ = callback(nil, nil, nil)
        } else if let error {
            _ = callback(nil, nil, error.localizedDescription)
        } else {
            _ = callback(result?.user.idToken?.tokenString, result?.user.accessToken.tokenString, nil)
        }
    }
}
