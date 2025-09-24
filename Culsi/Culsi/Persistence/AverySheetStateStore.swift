import Foundation
#if canImport(SwiftData)
import SwiftData
#endif

@MainActor
final class AverySheetStateStore {
    #if canImport(SwiftData)
    private let container: ModelContainer
    init(container: ModelContainer) { self.container = container }
    #else
    init() {}
    #endif

    nonisolated(unsafe) static let shared: AverySheetStateStore = {
        #if canImport(SwiftData)
        return AverySheetStateStore(container: try! CulsiDatabase.shared.container)
        #else
        return AverySheetStateStore()
        #endif
    }()

    #if canImport(SwiftData)
    func fetch() throws -> [AverySheetState] {
        try ModelContext(container).fetch(FetchDescriptor<AverySheetState>())
    }
    func upsert(_ state: AverySheetState) throws {
        let ctx = ModelContext(container); ctx.insert(state); try ctx.save()
    }
    func delete(_ state: AverySheetState) throws {
        let ctx = ModelContext(container); ctx.delete(state); try ctx.save()
    }
    #else
    func fetch() throws -> [AverySheetState] { [] }
    func upsert(_ state: AverySheetState) throws {}
    func delete(_ state: AverySheetState) throws {}
    #endif
}
