import Foundation
#if canImport(SwiftData)
import SwiftData

enum ItemCatalogStoreError: Error {
    case notFound
}

actor ItemCatalogStore {
    private let context: ModelContext

    init(database: Database = CulsiDatabase.shared) {
        context = database.newContext()
    }

    func fetchAll() throws -> [CatalogItem] {
        let descriptor = FetchDescriptor<CatalogItem>()
        let items = try context.fetch(descriptor)
        return items.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    @discardableResult
    func insert(_ item: CatalogItem) throws -> CatalogItem {
        context.insert(item)
        try context.save()
        return item
    }

    @discardableResult
    func upsert(from input: CatalogItemInput) throws -> CatalogItem {
        if let id = input.id, let existing = try context.fetch(FetchDescriptor<CatalogItem>(predicate: #Predicate { $0.id == id })).first {
            existing.name = input.name
            existing.defaultUnit = input.defaultUnit
            existing.notes = input.notes
            existing.lastUsed = input.lastUsed
            try context.save()
            return existing
        }
        let item = CatalogItem(
            id: input.id ?? UUID(),
            name: input.name,
            defaultUnit: input.defaultUnit,
            notes: input.notes,
            lastUsed: input.lastUsed
        )
        context.insert(item)
        try context.save()
        return item
    }

    func delete(id: UUID) throws {
        guard let existing = try context.fetch(FetchDescriptor<CatalogItem>(predicate: #Predicate { $0.id == id })).first else {
            throw ItemCatalogStoreError.notFound
        }
        context.delete(existing)
        try context.save()
    }
}
#else
// Core Data fallback can be implemented if SwiftData is unavailable.
#endif
