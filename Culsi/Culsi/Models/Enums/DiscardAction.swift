import Foundation

enum DiscardAction: String, CaseIterable, Codable {
    case keepUnused
    case discardUnused

    var localizedDescription: String {
        switch self {
        case .keepUnused: return "Keep unused labels"
        case .discardUnused: return "Discard unused labels"
        }
    }
}
