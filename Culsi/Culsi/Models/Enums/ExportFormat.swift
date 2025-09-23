import Foundation

enum ExportFormat: String, CaseIterable, Codable {
    case csv
    case json

    var fileExtension: String {
        switch self {
        case .csv: return "csv"
        case .json: return "json"
        }
    }

    var contentType: String {
        switch self {
        case .csv: return "text/csv"
        case .json: return "application/json"
        }
    }

    var label: String {
        rawValue.uppercased()
    }

    var systemImageName: String {
        switch self {
        case .csv: return "tablecells"
        case .json: return "curlybraces"
        }
    }
}
