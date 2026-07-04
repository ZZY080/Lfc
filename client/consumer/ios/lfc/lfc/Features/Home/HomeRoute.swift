import Foundation

enum HomeRoute: Hashable {
    case search
    case searchResult
    case notifications
    case notificationDetail(id: Int)
    case chat(conversationId: Int)
    case postDetail(id: Int)
    case activityDetail(id: Int)
    case productDetail(id: Int)
    case userProfile(id: Int)
    case publishPost
    case publishActivity
    case editPost
    case editActivity
    case locationSearch
    case settings
    case orders
    case paymentTransactions
    case myQrcode
    case profileQrScan
    case editProfile
    case profileSearch
    case legalDocument(id: String)
}

enum HomeTab: Int, CaseIterable, Identifiable {
    case discover = 0
    case activity = 1
    case messages = 2
    case profile = 3

    var id: Int { rawValue }

    var title: String {
        switch self {
        case .discover: return "首页"
        case .activity: return "活动"
        case .messages: return "消息"
        case .profile: return "我"
        }
    }

    var systemImage: String {
        switch self {
        case .discover: return "house.fill"
        case .activity: return "calendar"
        case .messages: return "message.fill"
        case .profile: return "person.fill"
        }
    }
}
