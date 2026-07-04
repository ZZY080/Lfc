import SwiftUI

struct LoginView: View {
    @StateObject private var viewModel: AuthViewModel
    let onLoginSuccess: () -> Void
    let onNavigateToRegister: () -> Void
    let onOpenLegalDocument: (LegalDocumentId) -> Void

    @State private var email = ""
    @State private var password = ""
    @State private var agreedToTerms = false
    @State private var showError = false
    @State private var errorMessage = ""

    init(
        session: SessionManager,
        onLoginSuccess: @escaping () -> Void,
        onNavigateToRegister: @escaping () -> Void,
        onOpenLegalDocument: @escaping (LegalDocumentId) -> Void
    ) {
        _viewModel = StateObject(wrappedValue: AuthViewModel(session: session))
        self.onLoginSuccess = onLoginSuccess
        self.onNavigateToRegister = onNavigateToRegister
        self.onOpenLegalDocument = onOpenLegalDocument
    }

    private var canSubmit: Bool {
        !viewModel.uiState.isLoading &&
            !email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
            password.count >= 6 &&
            agreedToTerms
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("欢迎回来")
                    .font(.title.bold())

                TextField("邮箱", text: $email)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .textFieldStyle(.roundedBorder)

                SecureField("密码", text: $password)
                    .textContentType(.password)
                    .textFieldStyle(.roundedBorder)

                LegalAgreementCheckbox(
                    isChecked: $agreedToTerms,
                    onOpenDocument: onOpenLegalDocument
                )

                Button {
                    Task {
                        await viewModel.login(
                            email: email.trimmingCharacters(in: .whitespacesAndNewlines),
                            password: password
                        )
                    }
                } label: {
                    Group {
                        if viewModel.uiState.isLoading {
                            ProgressView()
                                .tint(.white)
                        } else {
                            Text("登录")
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!canSubmit)

                Button("没有账号？去注册", action: onNavigateToRegister)
                    .frame(maxWidth: .infinity)
                    .buttonStyle(.bordered)
            }
            .padding(24)
        }
        .navigationTitle("登录")
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: viewModel.uiState.isSuccess) { _, isSuccess in
            if isSuccess {
                onLoginSuccess()
                viewModel.resetSuccess()
            }
        }
        .onChange(of: viewModel.uiState.error) { _, error in
            if let error {
                errorMessage = error
                showError = true
                viewModel.clearError()
            }
        }
        .alert("提示", isPresented: $showError) {
            Button("确定", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
    }
}

#Preview {
    NavigationStack {
        LoginView(
            session: .shared,
            onLoginSuccess: {},
            onNavigateToRegister: {},
            onOpenLegalDocument: { _ in }
        )
    }
    .environmentObject(SessionManager.shared)
}
