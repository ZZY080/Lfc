import SwiftUI

struct LfcToastBanner: View {
    let message: String
    let isError: Bool

    var body: some View {
        Text(message)
            .font(.system(size: 14, weight: .medium))
            .foregroundStyle(.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(
                (isError ? Color(red: 0.85, green: 0.2, blue: 0.2) : Color.black.opacity(0.78))
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            )
            .padding(.horizontal, 24)
            .padding(.top, 8)
            .shadow(color: .black.opacity(0.12), radius: 8, y: 4)
            .transition(.move(edge: .top).combined(with: .opacity))
    }
}

private struct LfcToastModifier: ViewModifier {
    let message: String?
    let isError: Bool
    let onDismiss: () -> Void

    @State private var visibleMessage: String?
    @State private var visibleIsError = false
    @State private var dismissTask: Task<Void, Never>?

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .top) {
                if let visibleMessage {
                    LfcToastBanner(message: visibleMessage, isError: visibleIsError)
                        .zIndex(999)
                }
            }
            .animation(.easeInOut(duration: 0.25), value: visibleMessage)
            .onChange(of: message) { _, newMessage in
                present(newMessage)
            }
            .onAppear {
                present(message)
            }
    }

    private func present(_ newMessage: String?) {
        guard let newMessage, !newMessage.isEmpty else { return }
        dismissTask?.cancel()
        visibleMessage = newMessage
        visibleIsError = isError
        dismissTask = Task {
            try? await Task.sleep(for: .seconds(isError ? 3 : 2))
            guard !Task.isCancelled else { return }
            visibleMessage = nil
            onDismiss()
        }
    }
}

extension View {
    func lfcToast(message: String?, isError: Bool = false, onDismiss: @escaping () -> Void) -> some View {
        modifier(LfcToastModifier(message: message, isError: isError, onDismiss: onDismiss))
    }

    /// Shows whichever toast is active — success takes priority over error.
    func lfcHomeToasts(message: String?, error: String?, onDismiss: @escaping () -> Void) -> some View {
        modifier(LfcHomeToastsModifier(message: message, error: error, onDismiss: onDismiss))
    }
}

private struct LfcHomeToastsModifier: ViewModifier {
    let message: String?
    let error: String?
    let onDismiss: () -> Void

    @State private var visibleMessage: String?
    @State private var visibleIsError = false
    @State private var dismissTask: Task<Void, Never>?

    private var activeText: String? { message ?? error }
    private var activeIsError: Bool { message == nil && error != nil }

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .top) {
                if let visibleMessage {
                    LfcToastBanner(message: visibleMessage, isError: visibleIsError)
                        .zIndex(999)
                }
            }
            .animation(.easeInOut(duration: 0.25), value: visibleMessage)
            .onChange(of: message) { _, _ in presentActive() }
            .onChange(of: error) { _, _ in presentActive() }
    }

    private func presentActive() {
        guard let text = activeText, !text.isEmpty else { return }
        dismissTask?.cancel()
        visibleMessage = text
        visibleIsError = activeIsError
        dismissTask = Task {
            try? await Task.sleep(for: .seconds(activeIsError ? 3 : 2))
            guard !Task.isCancelled else { return }
            visibleMessage = nil
            onDismiss()
        }
    }
}
