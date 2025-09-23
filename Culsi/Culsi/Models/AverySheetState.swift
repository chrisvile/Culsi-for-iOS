import Foundation
#if canImport(SwiftData)
import SwiftData

@Model
final class AverySheetState: Identifiable, Codable {
    @Attribute(.unique) var id: UUID
    var templateIdentifier: String
    var columns: Int
    var rows: Int
    var labelWidthInches: Double
    var labelHeightInches: Double
    var marginInches: Double
    @Attribute(.transformable) var placements: [AveryPlacement]
    var createdAt: Date
    var updatedAt: Date

    enum CodingKeys: CodingKey {
        case id
        case templateIdentifier
        case columns
        case rows
        case labelWidthInches
        case labelHeightInches
        case marginInches
        case placements
        case createdAt
        case updatedAt
    }

    init(
        id: UUID = UUID(),
        templateIdentifier: String,
        columns: Int = 3,
        rows: Int = 10,
        labelWidthInches: Double = 2.625,
        labelHeightInches: Double = 1.0,
        marginInches: Double = 0.25,
        placements: [AveryPlacement] = [],
        createdAt: Date = .now,
        updatedAt: Date = .now
    ) {
        self.id = id
        self.templateIdentifier = templateIdentifier
        self.columns = columns
        self.rows = rows
        self.labelWidthInches = labelWidthInches
        self.labelHeightInches = labelHeightInches
        self.marginInches = marginInches
        self.placements = placements
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }

    required convenience init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let id = try container.decode(UUID.self, forKey: .id)
        let templateIdentifier = try container.decode(String.self, forKey: .templateIdentifier)
        let columns = try container.decode(Int.self, forKey: .columns)
        let rows = try container.decode(Int.self, forKey: .rows)
        let labelWidthInches = try container.decode(Double.self, forKey: .labelWidthInches)
        let labelHeightInches = try container.decode(Double.self, forKey: .labelHeightInches)
        let marginInches = try container.decode(Double.self, forKey: .marginInches)
        let placements = try container.decode([AveryPlacement].self, forKey: .placements)
        let createdAt = try container.decode(Date.self, forKey: .createdAt)
        let updatedAt = try container.decode(Date.self, forKey: .updatedAt)
        self.init(
            id: id,
            templateIdentifier: templateIdentifier,
            columns: columns,
            rows: rows,
            labelWidthInches: labelWidthInches,
            labelHeightInches: labelHeightInches,
            marginInches: marginInches,
            placements: placements,
            createdAt: createdAt,
            updatedAt: updatedAt
        )
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(templateIdentifier, forKey: .templateIdentifier)
        try container.encode(columns, forKey: .columns)
        try container.encode(rows, forKey: .rows)
        try container.encode(labelWidthInches, forKey: .labelWidthInches)
        try container.encode(labelHeightInches, forKey: .labelHeightInches)
        try container.encode(marginInches, forKey: .marginInches)
        try container.encode(placements, forKey: .placements)
        try container.encode(createdAt, forKey: .createdAt)
        try container.encode(updatedAt, forKey: .updatedAt)
    }
}
#else
struct AverySheetState: Identifiable, Codable {
    var id: UUID
    var templateIdentifier: String
    var columns: Int
    var rows: Int
    var labelWidthInches: Double
    var labelHeightInches: Double
    var marginInches: Double
    var placements: [AveryPlacement]
    var createdAt: Date
    var updatedAt: Date

    init(
        id: UUID = UUID(),
        templateIdentifier: String,
        columns: Int = 3,
        rows: Int = 10,
        labelWidthInches: Double = 2.625,
        labelHeightInches: Double = 1.0,
        marginInches: Double = 0.25,
        placements: [AveryPlacement] = [],
        createdAt: Date = .now,
        updatedAt: Date = .now
    ) {
        self.id = id
        self.templateIdentifier = templateIdentifier
        self.columns = columns
        self.rows = rows
        self.labelWidthInches = labelWidthInches
        self.labelHeightInches = labelHeightInches
        self.marginInches = marginInches
        self.placements = placements
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}
#endif
