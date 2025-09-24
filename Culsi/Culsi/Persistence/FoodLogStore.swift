import Foundation
import SwiftData

struct FoodLogQuery: Equatable {
    var searchText: String?
    var startDate: Date?
    var endDate: Date?
}

enum FoodLogStoreError: Error {
    case notFound
}

actor FoodLogStore {
    private let context: ModelContext

    init(database: Database = CulsiDatabase.shared) {
        context = database.newContext()
    }

    func fetchLogs(matching query: FoodLogQuery = FoodLogQuery()) throws -> [FoodLog] {
        var descriptor = FetchDescriptor<FoodLog>(sortBy: [SortDescriptor(\FoodLog.startedAt, order: .reverse)])
        descriptor.fetchLimit = nil
        let results = try context.fetch(descriptor)
        guard query.searchText != nil || query.startDate != nil || query.endDate != nil else {
            return results
        }
        return results.filter { log in
            if let startDate = query.startDate, log.startedAt < startDate { return false }
            if let endDate = query.endDate, log.startedAt > endDate { return false }
            if let search = query.searchText?.trimmingCharacters(in: .whitespacesAndNewlines), !search.isEmpty {
                let matchesName = log.name.localizedCaseInsensitiveContains(search)
                let matchesLocation = (log.location ?? "").localizedCaseInsensitiveContains(search)
                let matchesEmployee = (log.employee ?? "").localizedCaseInsensitiveContains(search)
                let matchesNotes = (log.notes ?? "").localizedCaseInsensitiveContains(search)
                if !(matchesName || matchesNotes || matchesLocation || matchesEmployee) { return false }
            }
            return true
        }
    }

    @discardableResult
    func insert(_ input: FoodLogInput) throws -> FoodLog {
        let log = FoodLog(input: input)
        context.insert(log)
        try context.save()
        return log
    }

    @discardableResult
    func update(id: UUID, with input: FoodLogInput) throws -> FoodLog {
        guard let existing = try context.fetch(FetchDescriptor<FoodLog>(predicate: #Predicate { $0.id == id })).first else {
            throw FoodLogStoreError.notFound
        }
        existing.name = input.name
        existing.date = input.date
        existing.quantity = input.quantity
        existing.unit = input.unit
        existing.policy = input.policy
        existing.startedAt = input.startedAt
        existing.measuredTemp = input.measuredTemp
        existing.tempUnit = input.tempUnit
        existing.location = input.location
        existing.employee = input.employee
        existing.notes = input.notes
        try context.save()
        return existing
    }

    func delete(id: UUID) throws {
        guard let existing = try context.fetch(FetchDescriptor<FoodLog>(predicate: #Predicate { $0.id == id })).first else {
            throw FoodLogStoreError.notFound
        }
        context.delete(existing)
        try context.save()
    }

    func fetchActiveTPHC() throws -> [FoodLog] {
        try fetchTPHCLogs().filter { !$0.isExpired }
    }

    func fetchExpiredTPHC() throws -> [FoodLog] {
        try fetchTPHCLogs().filter(\.isExpired)
    }

    @discardableResult
    func updateTemperature(id: UUID, measuredTemp: Double?, unit: MeasureUnit) throws -> FoodLog {
        guard let existing = try context.fetch(FetchDescriptor<FoodLog>(predicate: #Predicate { $0.id == id })).first else {
            throw FoodLogStoreError.notFound
        }
        existing.measuredTemp = measuredTemp
        existing.tempUnit = unit
        try context.save()
        return existing
    }

    private func fetchTPHCLogs() throws -> [FoodLog] {
        let targetPolicy = HoldPolicy.tphc4h.rawValue
        let descriptor = FetchDescriptor<FoodLog>(
            predicate: #Predicate<FoodLog> { log in
                log.policy.rawValue == targetPolicy
            },
            sortBy: [SortDescriptor(\FoodLog.startedAt, order: .reverse)]
        )
        return try context.fetch(descriptor)
    }
}
