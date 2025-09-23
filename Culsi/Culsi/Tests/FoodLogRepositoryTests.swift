import XCTest
@testable import Culsi

final class FoodLogRepositoryTests: XCTestCase {
    func testCrudCycle() async throws {
        let database = CulsiDatabase(inMemory: true)
        let store = FoodLogStore(database: database)
        let repository = FoodLogRepository(store: store)

        try await repository.add(FoodLogInput(name: "Apple", quantity: 1, unit: "ea"))
        var logs = try await store.fetchLogs()
        XCTAssertEqual(logs.count, 1)

        if let original = logs.first {
            let updated = FoodLog(
                id: original.id,
                name: "Banana",
                date: original.date,
                quantity: original.quantity,
                unit: original.unit,
                notes: original.notes
            )
            try await repository.update(updated)
        }

        logs = try await store.fetchLogs()
        XCTAssertEqual(logs.first?.name, "Banana")

        if let identifier = logs.first?.id {
            try await repository.delete(id: identifier)
        }

        logs = try await store.fetchLogs()
        XCTAssertTrue(logs.isEmpty)
    }
}
