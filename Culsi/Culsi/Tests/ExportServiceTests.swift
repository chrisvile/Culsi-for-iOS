import XCTest
@testable import Culsi

final class ExportServiceTests: XCTestCase {
    private let service = ExportService()

    func testCSVContainsHeader() throws {
        let logs = [FoodLog(name: "Test", date: Date(timeIntervalSince1970: 0), quantity: 1.5, unit: "kg", notes: "Note")]
        let csv = service.csv(from: logs)
        XCTAssertTrue(csv.hasPrefix("id,name,date,quantity,unit,notes"))
    }

    func testCSVRowCountMatchesLogs() throws {
        let logs = [
            FoodLog(name: "One", quantity: 1, unit: "ea"),
            FoodLog(name: "Two", quantity: 2, unit: "ea")
        ]
        let csv = service.csv(from: logs)
        let rows = csv.split(separator: "\n")
        XCTAssertEqual(rows.count, logs.count + 1)
    }

    func testJSONExport() throws {
        let logs = [FoodLog(name: "Json", quantity: 2, unit: "L")]
        let data = try service.data(for: logs, format: .json)
        XCTAssertFalse(data.isEmpty)
    }
}
