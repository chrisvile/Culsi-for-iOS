import Foundation
#if canImport(SwiftData)
import SwiftData

protocol Database {
    var container: ModelContainer { get }
    func newContext() -> ModelContext
}

final class CulsiDatabase: Database {
    static let shared = CulsiDatabase()

    let container: ModelContainer

    init(inMemory: Bool = false) {
        do {
            let schema = Schema([
                FoodLog.self,
                CatalogItem.self,
                AverySheetState.self
            ])
            let configuration = ModelConfiguration(isStoredInMemoryOnly: inMemory)
            container = try ModelContainer(for: schema, configurations: configuration)
        } catch {
            fatalError("Unable to bootstrap database: \(error)")
        }
    }

    func newContext() -> ModelContext {
        ModelContext(container)
    }

    func seedIfNeeded(dateProvider: () -> Date = { .now }) {
        #if DEBUG
        let context = ModelContext(container)
        let logCount = (try? context.fetch(FetchDescriptor<FoodLog>()).count) ?? 0
        if logCount == 0 {
            let sampleItems = [
                CatalogItem(name: "Milk", defaultUnit: "L", notes: "Whole"),
                CatalogItem(name: "Eggs", defaultUnit: "ea"),
                CatalogItem(name: "Bread", defaultUnit: "loaf")
            ]
            sampleItems.forEach { context.insert($0) }

            let now = dateProvider()
            let logs = [
                FoodLog(name: "Milk", date: now, quantity: 2, unit: "L", notes: "Organic"),
                FoodLog(name: "Eggs", date: now.addingTimeInterval(-86400), quantity: 12, unit: "ea", notes: "Farm"),
                FoodLog(name: "Bread", date: now.addingTimeInterval(-172800), quantity: 1, unit: "loaf")
            ]
            logs.forEach { context.insert($0) }

            let sheet = AverySheetState(templateIdentifier: LabelTemplates.avery5160.id)
            context.insert(sheet)
            try? context.save()
        }
        #endif
    }
}
#else
// Core Data fallback can be implemented if SwiftData is unavailable.
#endif
