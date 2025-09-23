import Foundation
#if canImport(SwiftData)
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
        var descriptor = FetchDescriptor<FoodLog>(sortBy: [SortDescriptor(\FoodLog.date, order: .reverse)])
        descriptor.fetchLimit = nil
        let results = try context.fetch(descriptor)
        guard query.searchText != nil || query.startDate != nil || query.endDate != nil else {
            return results
        }
        return results.filter { log in
            if let startDate = query.startDate, log.date < startDate { return false }
            if let endDate = query.endDate, log.date > endDate { return false }
            if let search = query.searchText?.trimmingCharacters(in: .whitespacesAndNewlines), !search.isEmpty {
                let matchesName = log.name.localizedCaseInsensitiveContains(search)
                let matchesNotes = (log.notes ?? "").localizedCaseInsensitiveContains(search)
                if !(matchesName || matchesNotes) { return false }
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
}
#else
// Core Data fallback can be implemented if SwiftData is unavailable.
#endif
