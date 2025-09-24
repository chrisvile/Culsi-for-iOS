import Foundation
#if canImport(SwiftData)
import SwiftData

protocol Database {
    var container: ModelContainer { get }
    func newContext() -> ModelContext
}

@available(iOS 17.0, *)
final class CulsiDatabase: Database {
    static let shared = CulsiDatabase()

    let container: ModelContainer

    init(inMemory: Bool = false) {
        let schema = Schema([
            FoodLog.self,
            CatalogItem.self,
            AverySheetState.self
        ])
        let configuration = ModelConfiguration(isStoredInMemoryOnly: inMemory)

        do {
            container = try ModelContainer(for: schema, configurations: configuration)
        } catch {
            assertionFailure("Unable to bootstrap persistent database: \(error)")

            guard !inMemory else {
                fatalError("Unable to bootstrap database: \(error)")
            }

            let fallbackConfiguration = ModelConfiguration(isStoredInMemoryOnly: true)

            do {
                container = try ModelContainer(for: schema, configurations: fallbackConfiguration)
            } catch {
                fatalError("Unable to bootstrap fallback database: \(error)")
            }
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
                FoodLog(
                    name: "Soup",
                    date: now,
                    quantity: 1,
                    unit: "pan",
                    policy: .hotHold,
                    startedAt: now.addingTimeInterval(-1800),
                    measuredTemp: 165,
                    tempUnit: .f,
                    location: "Line 1",
                    employee: "AB",
                    notes: "Stir hourly"
                ),
                FoodLog(
                    name: "Salad",
                    date: now.addingTimeInterval(-86400),
                    quantity: 3,
                    unit: "tray",
                    policy: .coldHold,
                    startedAt: now.addingTimeInterval(-7200),
                    measuredTemp: 41,
                    tempUnit: .f,
                    location: "Salad Bar",
                    employee: "CD",
                    notes: "Keep covered"
                ),
                FoodLog(
                    name: "Pizza",
                    date: now.addingTimeInterval(-172800),
                    quantity: 10,
                    unit: "slice",
                    policy: .tphc4h,
                    startedAt: now.addingTimeInterval(-3000),
                    measuredTemp: nil,
                    tempUnit: .f,
                    location: "Grab & Go"
                )
            ]
            logs.forEach { context.insert($0) }

            let sheet = AverySheetState()
            context.insert(sheet)
            try? context.save()
        }
        #endif
    }
}
#else
// Core Data fallback can be implemented if SwiftData is unavailable.
#endif
