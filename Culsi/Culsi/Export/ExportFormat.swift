import Foundation
import UniformTypeIdentifiers

enum ExportFormat: String, Codable {
    case csv, json
    var fileExtension: String { self == .csv ? "csv" : "json" }
    var utType: UTType { self == .csv ? .commaSeparatedText : .json }
}
