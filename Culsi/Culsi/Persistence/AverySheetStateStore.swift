import Foundation
#if canImport(SwiftData)
import SwiftData

enum AverySheetStoreError: Error {
    case notFound
}

actor AverySheetStateStore {
    private let context: ModelContext

    init(database: Database = CulsiDatabase.shared) {
        context = database.newContext()
    }

    func loadCurrent(templateIdentifier: String) throws -> AverySheetState {
        if let existing = try context.fetch(FetchDescriptor<AverySheetState>()).first {
            return existing
        }
        let state = AverySheetState(templateIdentifier: templateIdentifier)
        context.insert(state)
        try context.save()
        return state
    }

    func save(_ state: AverySheetState) throws {
        state.updatedAt = .now
        try context.save()
    }

    func reset(templateIdentifier: String) throws -> AverySheetState {
        let descriptor = FetchDescriptor<AverySheetState>()
        let states = try context.fetch(descriptor)
        for state in states {
            context.delete(state)
        }
        let newState = AverySheetState(templateIdentifier: templateIdentifier)
        context.insert(newState)
        try context.save()
        return newState
    }
}
#else
// Core Data fallback can be implemented if SwiftData is unavailable.
#endif
