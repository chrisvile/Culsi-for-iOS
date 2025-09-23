import Foundation
#if canImport(SwiftData)
import SwiftData

@Model
final class CatalogItem: Identifiable, Codable {
    @Attribute(.unique) var id: UUID
    var name: String
    var defaultUnit: String
    var notes: String?
    var lastUsed: Date?

    enum CodingKeys: CodingKey {
        case id, name, defaultUnit, notes, lastUsed
    }

    init(
        id: UUID = UUID(),
        name: String,
        defaultUnit: String = "ea",
        notes: String? = nil,
        lastUsed: Date? = nil
    ) {
        self.id = id
        self.name = name
        self.defaultUnit = defaultUnit
        self.notes = notes
        self.lastUsed = lastUsed
    }

    required convenience init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let id = try container.decode(UUID.self, forKey: .id)
        let name = try container.decode(String.self, forKey: .name)
        let unit = try container.decode(String.self, forKey: .defaultUnit)
        let notes = try container.decodeIfPresent(String.self, forKey: .notes)
        let lastUsed = try container.decodeIfPresent(Date.self, forKey: .lastUsed)
        self.init(id: id, name: name, defaultUnit: unit, notes: notes, lastUsed: lastUsed)
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(name, forKey: .name)
        try container.encode(defaultUnit, forKey: .defaultUnit)
        try container.encodeIfPresent(notes, forKey: .notes)
        try container.encodeIfPresent(lastUsed, forKey: .lastUsed)
    }
}

struct CatalogItemInput: Identifiable, Equatable, Codable {
    var id: UUID?
    var name: String
    var defaultUnit: String
    var notes: String?
    var lastUsed: Date?

    init(
        id: UUID? = nil,
        name: String = "",
        defaultUnit: String = "ea",
        notes: String? = nil,
        lastUsed: Date? = nil
    ) {
        self.id = id
        self.name = name
        self.defaultUnit = defaultUnit
        self.notes = notes
        self.lastUsed = lastUsed
    }

    init(item: CatalogItem) {
        self.init(id: item.id, name: item.name, defaultUnit: item.defaultUnit, notes: item.notes, lastUsed: item.lastUsed)
    }
}
#else
// Core Data fallback can be implemented if SwiftData is unavailable.
#endif
