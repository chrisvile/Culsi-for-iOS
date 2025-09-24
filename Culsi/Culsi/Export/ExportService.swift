import Foundation
import UniformTypeIdentifiers
import CoreTransferable

enum ExportError: Error { case encoding }

struct ExportService {
    func data(for logs: [FoodLog], format: ExportFormat) throws -> Data {
        switch format {
        case .csv:
            guard let out = csv(from: logs).data(using: .utf8) else { throw ExportError.encoding }
            return out
        case .json:
            let enc = JSONEncoder()
            enc.outputFormatting = [.prettyPrinted, .sortedKeys]
            enc.dateEncodingStrategy = .iso8601
            return try enc.encode(logs)
        }
    }

    func payload(for logs: [FoodLog], format: ExportFormat, filename: String = "food-log") throws -> ExportPayload {
        ExportPayload(data: try data(for: logs, format: format), filename: filename, format: format)
    }

    func csv(from logs: [FoodLog]) -> String {
        var rows = ["id,name,date,quantity,unit,notes,policy,startedAt,measuredTemp,tempUnit,location,employee,expiresAt,isExpired"]
        for log in logs {
            let values = [
                log.id.uuidString,
                escape(log.name),
                Converters.isoFormatter.string(from: log.date),
                String(log.quantity),
                escape(log.unit),
                escape(log.notes ?? ""),
                log.policy.rawValue,
                Converters.isoFormatter.string(from: log.resolvedStartedAt),
                log.measuredTemp.map { String($0) } ?? "",
                log.tempUnit.rawValue,
                escape(log.location ?? ""),
                escape(log.employee ?? ""),
                log.policy == .tphc4h ? Converters.isoFormatter.string(from: log.expiresAt) : "",
                log.policy == .tphc4h ? String(log.isExpired) : ""
            ]
            rows.append(values.joined(separator: ","))
        }
        return rows.joined(separator: "\n")
    }

    private func escape(_ s: String) -> String {
        if s.contains(",") || s.contains("\"") { return "\"\(s.replacingOccurrences(of: "\"", with: "\"\""))\"" }
        return s
    }
}

@available(iOS 16.0, *)
struct ExportPayload: Transferable {
    let data: Data
    let filename: String
    let format: ExportFormat

    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(exportedContentType: .data) { $0.data }
            .suggestedFileName { "\($0.filename).\($0.format.fileExtension)" }
    }
}
