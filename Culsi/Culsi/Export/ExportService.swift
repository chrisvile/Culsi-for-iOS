import Foundation
import UniformTypeIdentifiers

enum ExportError: Error {
    case encoding
}

struct ExportService {
    func data(for logs: [FoodLog], format: ExportFormat) throws -> Data {
        switch format {
        case .csv:
            guard let data = csv(from: logs).data(using: .utf8) else { throw ExportError.encoding }
            return data
        case .json:
            let encoder = JSONEncoder()
            encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
            encoder.dateEncodingStrategy = .iso8601
            return try encoder.encode(logs)
        }
    }

    func payload(for logs: [FoodLog], format: ExportFormat, filename: String = "food-log") throws -> ExportPayload {
        let data = try data(for: logs, format: format)
        return ExportPayload(data: data, filename: filename, format: format)
    }

    func csv(from logs: [FoodLog]) -> String {
        var rows = ["id,name,date,quantity,unit,notes"]
        for log in logs {
            let values = [
                log.id.uuidString,
                escape(log.name),
                Converters.isoFormatter.string(from: log.date),
                String(log.quantity),
                escape(log.unit),
                escape(log.notes ?? "")
            ]
            rows.append(values.joined(separator: ","))
        }
        return rows.joined(separator: "\n")
    }

    private func escape(_ value: String) -> String {
        if value.contains(",") || value.contains("\"") {
            let escaped = value.replacingOccurrences(of: "\"", with: "\"\"")
            return "\"\(escaped)\""
        }
        return value
    }
}

struct ExportPayload: Transferable {
    let data: Data
    let filename: String
    let format: ExportFormat

    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(exportedContentType: .plainText) { payload in
            payload.data
        }
        .suggestedFileName { payload in
            "\(payload.filename).\(payload.format.fileExtension)"
        }
    }
}
