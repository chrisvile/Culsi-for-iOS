import Foundation
#if canImport(SwiftData)
import SwiftData

@Model
final class FoodLog: Identifiable, Codable {
    @Attribute(.unique) var id: UUID
    var name: String
    var date: Date
    var quantity: Double
    var unit: String
    var notes: String?

    enum CodingKeys: CodingKey {
        case id, name, date, quantity, unit, notes
    }

    init(
        id: UUID = UUID(),
        name: String,
        date: Date = .now,
        quantity: Double,
        unit: String,
        notes: String? = nil
    ) {
        self.id = id
        self.name = name
        self.date = date
        self.quantity = quantity
        self.unit = unit
        self.notes = notes
    }

    convenience init(input: FoodLogInput) {
        self.init(
            id: input.id ?? UUID(),
            name: input.name,
            date: input.date,
            quantity: input.quantity,
            unit: input.unit,
            notes: input.notes
        )
    }

    required convenience init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let id = try container.decode(UUID.self, forKey: .id)
        let name = try container.decode(String.self, forKey: .name)
        let date = try container.decode(Date.self, forKey: .date)
        let quantity = try container.decode(Double.self, forKey: .quantity)
        let unit = try container.decode(String.self, forKey: .unit)
        let notes = try container.decodeIfPresent(String.self, forKey: .notes)
        self.init(id: id, name: name, date: date, quantity: quantity, unit: unit, notes: notes)
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(name, forKey: .name)
        try container.encode(date, forKey: .date)
        try container.encode(quantity, forKey: .quantity)
        try container.encode(unit, forKey: .unit)
        try container.encodeIfPresent(notes, forKey: .notes)
    }
}

struct FoodLogInput: Identifiable, Equatable, Codable {
    var id: UUID?
    var name: String
    var date: Date
    var quantity: Double
    var unit: String
    var notes: String?

    init(
        id: UUID? = nil,
        name: String = "",
        date: Date = .now,
        quantity: Double = 1,
        unit: String = "ea",
        notes: String? = nil
    ) {
        self.id = id
        self.name = name
        self.date = date
        self.quantity = quantity
        self.unit = unit
        self.notes = notes
    }

    init(log: FoodLog) {
        self.init(id: log.id, name: log.name, date: log.date, quantity: log.quantity, unit: log.unit, notes: log.notes)
    }
}
#else
// Core Data fallback can be implemented if SwiftData is unavailable.
#endif
