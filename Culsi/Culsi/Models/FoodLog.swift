import Foundation
#if canImport(SwiftData)
import SwiftData

enum HoldPolicy: String, Codable, CaseIterable, Identifiable {
    case hotHold
    case coldHold
    case tphc4h

    var id: String { rawValue }

    var title: String {
        switch self {
        case .hotHold:
            return "Hot Hold"
        case .coldHold:
            return "Cold Hold"
        case .tphc4h:
            return "TPHC (4h)"
        }
    }

    var systemImage: String {
        switch self {
        case .hotHold:
            return "thermometer.sun"
        case .coldHold:
            return "thermometer.snowflake"
        case .tphc4h:
            return "timer"
        }
    }
}

enum MeasureUnit: String, Codable, CaseIterable, Hashable {
    case f
    case c
    case ea
}

extension MeasureUnit {
    var title: String {
        switch self {
        case .f:
            return "°F"
        case .c:
            return "°C"
        case .ea:
            return "ea"
        }
    }
}

@Model
final class FoodLog: Identifiable, Codable {
    @Attribute(.unique) var id: UUID
    var name: String
    var date: Date
    var quantity: Double
    var unit: String
    var policy: HoldPolicy
    var startedAt: Date?
    var measuredTemp: Double?
    var tempUnit: MeasureUnit
    var location: String?
    var employee: String?
    var notes: String?

    var resolvedStartedAt: Date {
        get { startedAt ?? date }
        set { startedAt = newValue }
    }

    var expiresAt: Date {
        policy == .tphc4h ? resolvedStartedAt.addingTimeInterval(4 * 60 * 60) : resolvedStartedAt
    }

    var isExpired: Bool {
        policy == .tphc4h && Date() >= expiresAt
    }

    enum CodingKeys: CodingKey {
        case id, name, date, quantity, unit, policy, startedAt, measuredTemp, tempUnit, location, employee, notes
    }

    init(
        id: UUID = UUID(),
        name: String,
        date: Date = .now,
        quantity: Double,
        unit: String,
        policy: HoldPolicy,
        startedAt: Date? = nil,
        measuredTemp: Double? = nil,
        tempUnit: MeasureUnit = .f,
        location: String? = nil,
        employee: String? = nil,
        notes: String? = nil
    ) {
        self.id = id
        self.name = name
        self.date = date
        self.quantity = quantity
        self.unit = unit
        self.policy = policy
        self.startedAt = startedAt ?? date
        self.measuredTemp = measuredTemp
        self.tempUnit = tempUnit
        self.location = location
        self.employee = employee
        self.notes = notes
    }

    convenience init(input: FoodLogInput) {
        self.init(
            id: input.id ?? UUID(),
            name: input.name,
            date: input.date,
            quantity: input.quantity,
            unit: input.unit,
            policy: input.policy,
            startedAt: input.startedAt,
            measuredTemp: input.measuredTemp,
            tempUnit: input.tempUnit,
            location: input.location,
            employee: input.employee,
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
        let policy = try container.decodeIfPresent(HoldPolicy.self, forKey: .policy) ?? .hotHold
        let startedAt = try container.decodeIfPresent(Date.self, forKey: .startedAt)
        let measuredTemp = try container.decodeIfPresent(Double.self, forKey: .measuredTemp)
        let tempUnit = try container.decodeIfPresent(MeasureUnit.self, forKey: .tempUnit) ?? .f
        let location = try container.decodeIfPresent(String.self, forKey: .location)
        let employee = try container.decodeIfPresent(String.self, forKey: .employee)
        let notes = try container.decodeIfPresent(String.self, forKey: .notes)
        self.init(
            id: id,
            name: name,
            date: date,
            quantity: quantity,
            unit: unit,
            policy: policy,
            startedAt: startedAt,
            measuredTemp: measuredTemp,
            tempUnit: tempUnit,
            location: location,
            employee: employee,
            notes: notes
        )
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(name, forKey: .name)
        try container.encode(date, forKey: .date)
        try container.encode(quantity, forKey: .quantity)
        try container.encode(unit, forKey: .unit)
        try container.encode(policy, forKey: .policy)
        try container.encode(resolvedStartedAt, forKey: .startedAt)
        try container.encodeIfPresent(measuredTemp, forKey: .measuredTemp)
        try container.encode(tempUnit, forKey: .tempUnit)
        try container.encodeIfPresent(location, forKey: .location)
        try container.encodeIfPresent(employee, forKey: .employee)
        try container.encodeIfPresent(notes, forKey: .notes)
    }
}

struct FoodLogInput: Identifiable, Equatable, Codable {
    var id: UUID?
    var name: String
    var date: Date
    var quantity: Double
    var unit: String
    var policy: HoldPolicy
    var startedAt: Date
    var measuredTemp: Double?
    var tempUnit: MeasureUnit
    var location: String?
    var employee: String?
    var notes: String?

    init(
        id: UUID? = nil,
        name: String = "",
        date: Date = .now,
        quantity: Double = 1,
        unit: String = "ea",
        policy: HoldPolicy = .hotHold,
        startedAt: Date = .now,
        measuredTemp: Double? = nil,
        tempUnit: MeasureUnit = .f,
        location: String? = nil,
        employee: String? = nil,
        notes: String? = nil
    ) {
        self.id = id
        self.name = name
        self.date = date
        self.quantity = quantity
        self.unit = unit
        self.policy = policy
        self.startedAt = startedAt
        self.measuredTemp = measuredTemp
        self.tempUnit = tempUnit
        self.location = location
        self.employee = employee
        self.notes = notes
    }

    init(log: FoodLog) {
        self.init(
            id: log.id,
            name: log.name,
            date: log.date,
            quantity: log.quantity,
            unit: log.unit,
            policy: log.policy,
            startedAt: log.resolvedStartedAt,
            measuredTemp: log.measuredTemp,
            tempUnit: log.tempUnit,
            location: log.location,
            employee: log.employee,
            notes: log.notes
        )
    }
}
#else
// Core Data fallback can be implemented if SwiftData is unavailable.
#endif
