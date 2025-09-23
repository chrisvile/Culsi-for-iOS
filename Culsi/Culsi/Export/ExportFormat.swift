import Foundation
import UniformTypeIdentifiers

enum ExportFormat: String, CaseIterable, Codable {
    case csv
    case json

    var fileExtension: String {
        switch self {
        case .csv: return "csv"
        case .json: return "json"
        }
    }

    var utType: UTType {
        switch self {
        case .csv: return .commaSeparatedText
        case .json: return .json
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
