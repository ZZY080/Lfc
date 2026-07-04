import Foundation
import PhotosUI

extension HomeStore {
    func searchMyPosts(_ keyword: String) async {
        let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        profileSearchState.keyword = trimmed
        profileSearchState.isLoading = true
        profileSearchState.hasSearched = true

        do {
            profileSearchState.results = try await api.getMyPosts(
                keyword: trimmed.nilIfBlank
            )
        } catch {
            profileSearchState.results = []
            toastError = parseError(error, fallback: "搜索失败")
        }

        profileSearchState.isLoading = false
    }

    func lookupProfileByLfcNo(_ lfcNo: String) async -> UserProfileDto? {
        let trimmed = lfcNo.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        do {
            return try await api.getUserProfileByLfcNo(lfcNo: trimmed)
        } catch {
            toastError = parseError(error, fallback: "未找到该用户")
            return nil
        }
    }

    func updateProfile(
        nickname: String,
        bio: String,
        avatarData: ImageUploadPayload?,
        onSuccess: @escaping () -> Void
    ) async {
        isProfileUpdating = true
        defer { isProfileUpdating = false }

        do {
            var avatarUrl = profileState.myProfile?.avatarUrl
            if let avatarData {
                avatarUrl = try await api.uploadImage(
                    data: avatarData.data,
                    filename: avatarData.filename,
                    mimeType: "image/jpeg",
                    scope: "profile"
                ).url
            }

            let updated = try await api.updateMyProfile(
                UpdateProfileRequest(
                    nickname: nickname.trimmingCharacters(in: .whitespacesAndNewlines).nilIfBlank,
                    bio: bio.trimmingCharacters(in: .whitespacesAndNewlines).nilIfBlank,
                    avatarUrl: avatarUrl,
                    coverUrl: nil,
                    showCommentsPublic: nil,
                    showFavoritesPublic: nil,
                    showLikesPublic: nil
                )
            )
            profileState.myProfile = updated
            profileState.profileNotes = updated.posts
            profileState.profileActivities = updated.activities
            toastMessage = "主页已更新"
            onSuccess()
        } catch {
            toastError = parseError(error, fallback: "更新主页失败")
        }
    }

    func updatePrivacySettings(
        showCommentsPublic: Bool,
        showFavoritesPublic: Bool,
        showLikesPublic: Bool
    ) async {
        isPrivacyUpdating = true
        defer { isPrivacyUpdating = false }

        do {
            let updated = try await api.updateMyProfile(
                UpdateProfileRequest(
                    nickname: nil,
                    bio: nil,
                    avatarUrl: nil,
                    coverUrl: nil,
                    showCommentsPublic: showCommentsPublic,
                    showFavoritesPublic: showFavoritesPublic,
                    showLikesPublic: showLikesPublic
                )
            )
            profileState.myProfile = updated
            toastMessage = "隐私设置已保存"
        } catch {
            toastError = parseError(error, fallback: "保存隐私设置失败")
        }
    }

    func unbindAlipayAccount() async {
        isPrivacyUpdating = true
        defer { isPrivacyUpdating = false }

        do {
            _ = try await api.unbindMyAlipayAccount()
            await loadProfile()
            toastMessage = "已解绑收款账号"
        } catch {
            toastError = parseError(error, fallback: "解绑失败")
        }
    }
}

private extension String {
    var nilIfBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
