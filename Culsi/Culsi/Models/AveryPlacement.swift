import Foundation

struct AveryPlacement: Identifiable, Codable, Hashable {
    var id: UUID
    var row: Int
    var column: Int
    var text: String

    init(id: UUID = UUID(), row: Int, column: Int, text: String) {
        self.id = id
        self.row = row
        self.column = column
        self.text = text
    }
}
