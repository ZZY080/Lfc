import PhotosUI
import SwiftUI
import UniformTypeIdentifiers

private let alipayBindHintForSellers =
    "若要挂载商品或收取活动费，请在「我的 → 设置」跳转支付宝 App 完成授权"

struct RegisterView: View {
    @StateObject private var viewModel: AuthViewModel
    let onRegisterSuccess: () -> Void
    let onNavigateToLogin: () -> Void
    let onOpenLegalDocument: (LegalDocumentId) -> Void

    @State private var email = ""
    @State private var password = ""
    @State private var realName = ""
    @State private var studentId = ""
    @State private var agreedToTerms = false
    @State private var showError = false
    @State private var errorMessage = ""

    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var studentCardPreview: UIImage?
    @State private var studentCardData: Data?
    @State private var studentCardFileName: String?
    @State private var studentCardMimeType: String?

    init(
        session: SessionManager,
        onRegisterSuccess: @escaping () -> Void,
        onNavigateToLogin: @escaping () -> Void,
        onOpenLegalDocument: @escaping (LegalDocumentId) -> Void
    ) {
        _viewModel = StateObject(wrappedValue: AuthViewModel(session: session))
        self.onRegisterSuccess = onRegisterSuccess
        self.onNavigateToLogin = onNavigateToLogin
        self.onOpenLegalDocument = onOpenLegalDocument
    }

    private var canSubmit: Bool {
        !viewModel.uiState.isLoading &&
            !email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
            password.count >= 6 &&
            realName.trimmingCharacters(in: .whitespacesAndNewlines).count >= 2 &&
            !studentId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
            agreedToTerms
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("学生注册")
                    .font(.title.bold())

                TextField("邮箱", text: $email)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .textFieldStyle(.roundedBorder)

                SecureField("密码（至少6位）", text: $password)
                    .textContentType(.newPassword)
                    .textFieldStyle(.roundedBorder)

                TextField("真实姓名", text: $realName)
                    .textContentType(.name)
                    .textFieldStyle(.roundedBorder)

                TextField("学号", text: $studentId)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .textFieldStyle(.roundedBorder)

                PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                    Text(studentCardData == nil ? "上传学生证照片" : "已选择学生证照片")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .onChange(of: selectedPhotoItem) { _, newItem in
                    Task { await loadStudentCard(from: newItem) }
                }

                if let studentCardPreview {
                    Image(uiImage: studentCardPreview)
                        .resizable()
                        .scaledToFill()
                        .frame(maxWidth: .infinity)
                        .frame(height: 180)
                        .clipShape(RoundedRectangle(cornerRadius: 12))

                    if let studentCardFileName {
                        Text("已选择：\(studentCardFileName)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Text(alipayBindHintForSellers)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineSpacing(3)

                LegalAgreementCheckbox(
                    isChecked: $agreedToTerms,
                    onOpenDocument: onOpenLegalDocument
                )

                Button {
                    Task {
                        await viewModel.register(
                            email: email.trimmingCharacters(in: .whitespacesAndNewlines),
                            password: password,
                            realName: realName.trimmingCharacters(in: .whitespacesAndNewlines),
                            studentId: studentId.trimmingCharacters(in: .whitespacesAndNewlines),
                            studentCardData: studentCardData,
                            studentCardFileName: studentCardFileName,
                            studentCardMimeType: studentCardMimeType
                        )
                    }
                } label: {
                    Group {
                        if viewModel.uiState.isLoading {
                            ProgressView()
                                .tint(.white)
                        } else {
                            Text("注册")
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!canSubmit)

                Button("已有账号？去登录", action: onNavigateToLogin)
                    .frame(maxWidth: .infinity)
                    .buttonStyle(.bordered)
            }
            .padding(24)
        }
        .navigationTitle("注册")
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: viewModel.uiState.isSuccess) { _, isSuccess in
            if isSuccess {
                onRegisterSuccess()
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

    private func loadStudentCard(from item: PhotosPickerItem?) async {
        guard let item else {
            studentCardPreview = nil
            studentCardData = nil
            studentCardFileName = nil
            studentCardMimeType = nil
            return
        }

        if let data = try? await item.loadTransferable(type: Data.self),
           let image = UIImage(data: data) {
            let jpegData = image.jpegData(compressionQuality: 0.85) ?? data
            studentCardPreview = image
            studentCardData = jpegData
            studentCardFileName = "student_card.jpg"
            studentCardMimeType = UTType.jpeg.preferredMIMEType ?? "image/jpeg"
            return
        }

        studentCardPreview = nil
        studentCardData = nil
        studentCardFileName = nil
        studentCardMimeType = nil
    }
}

#Preview {
    NavigationStack {
        RegisterView(
            session: .shared,
            onRegisterSuccess: {},
            onNavigateToLogin: {},
            onOpenLegalDocument: { _ in }
        )
    }
    .environmentObject(SessionManager.shared)
}
