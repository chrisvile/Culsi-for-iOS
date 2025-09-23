import Foundation
#if canImport(SwiftData)
import SwiftData

@Model
final class AverySheetState: Identifiable, Codable {
    @Attribute(.unique) var id: UUID
    var templateIdentifier: String
    @Attribute(.transformable) var placements: [AveryPlacement]
    var createdAt: Date
    var updatedAt: Date

    enum CodingKeys: CodingKey {
        case id, templateIdentifier, placements, createdAt, updatedAt
    }

    init(
        id: UUID = UUID(),
        templateIdentifier: String,
        placements: [AveryPlacement] = [],
        createdAt: Date = .now,
        updatedAt: Date = .now
    ) {
        self.id = id
        self.templateIdentifier = templateIdentifier
        self.placements = placements
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }

    required convenience init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let id = try container.decode(UUID.self, forKey: .id)
        let templateIdentifier = try container.decode(String.self, forKey: .templateIdentifier)
        let placements = try container.decode([AveryPlacement].self, forKey: .placements)
        let createdAt = try container.decode(Date.self, forKey: .createdAt)
        let updatedAt = try container.decode(Date.self, forKey: .updatedAt)
        self.init(
            id: id,
            templateIdentifier: templateIdentifier,
            placements: placements,
            createdAt: createdAt,
            updatedAt: updatedAt
        )
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(templateIdentifier, forKey: .templateIdentifier)
        try container.encode(placements, forKey: .placements)
        try container.encode(createdAt, forKey: .createdAt)
        try container.encode(updatedAt, forKey: .updatedAt)
    }
}
#else
// Core Data fallback can be implemented if SwiftData is unavailable.
#endif
