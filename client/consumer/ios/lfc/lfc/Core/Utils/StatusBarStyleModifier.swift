import SwiftUI
import UIKit

enum AppStatusBarStyle {
    case `default`
    case lightContent
    case darkContent

    var uiKitStyle: UIStatusBarStyle {
        switch self {
        case .default: .default
        case .lightContent: .lightContent
        case .darkContent: .darkContent
        }
    }
}

private final class StatusBarHostController: UIViewController {
    var style: UIStatusBarStyle = .default {
        didSet { setNeedsStatusBarAppearanceUpdate() }
    }

    override var preferredStatusBarStyle: UIStatusBarStyle { style }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.isUserInteractionEnabled = false
        view.backgroundColor = .clear
    }
}

private struct StatusBarStyleConfigurator: UIViewControllerRepresentable {
    let style: UIStatusBarStyle

    func makeUIViewController(context: Context) -> StatusBarHostController {
        let controller = StatusBarHostController()
        controller.style = style
        return controller
    }

    func updateUIViewController(_ controller: StatusBarHostController, context: Context) {
        controller.style = style
    }
}

struct StatusBarStyleModifier: ViewModifier {
    let style: AppStatusBarStyle

    func body(content: Content) -> some View {
        content.background(StatusBarStyleConfigurator(style: style.uiKitStyle))
    }
}

extension View {
    func appStatusBarStyle(_ style: AppStatusBarStyle) -> some View {
        modifier(StatusBarStyleModifier(style: style))
    }
}
