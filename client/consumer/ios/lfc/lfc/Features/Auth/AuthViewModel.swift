import Foundation

struct AuthUiState: Equatable {
    var isLoading = false
    var error: String?
    var isSuccess = false
}

@MainActor
final class AuthViewModel: ObservableObject {
    @Published private(set) var uiState = AuthUiState()

    private let session: SessionManager

    init(session: SessionManager) {
        self.session = session
    }

    func login(email: String, password: String) async {
        uiState = AuthUiState(isLoading: true)
        do {
            try await session.login(email: email, password: password)
            uiState = AuthUiState(isSuccess: true)
        } catch {
            uiState = AuthUiState(error: session.parseErrorMessage(from: error, fallback: "登录失败"))
        }
    }

    func register(
        email: String,
        password: String,
        realName: String,
        studentId: String,
        studentCardData: Data?,
        studentCardFileName: String?,
        studentCardMimeType: String?
    ) async {
        if realName.count < 2 {
            uiState = AuthUiState(error: "请填写真实姓名")
            return
        }
        guard let studentCardData, let studentCardFileName, let studentCardMimeType else {
            uiState = AuthUiState(error: "请上传学生证照片")
            return
        }

        uiState = AuthUiState(isLoading: true)
        do {
            try await session.register(
                email: email,
                password: password,
                realName: realName,
                studentId: studentId,
                studentCardData: studentCardData,
                studentCardFilename: studentCardFileName,
                studentCardMimeType: studentCardMimeType
            )
            uiState = AuthUiState(isSuccess: true)
        } catch {
            uiState = AuthUiState(error: session.parseErrorMessage(from: error, fallback: "注册失败"))
        }
    }

    func clearError() {
        uiState.error = nil
    }

    func resetSuccess() {
        uiState.isSuccess = false
    }
}
