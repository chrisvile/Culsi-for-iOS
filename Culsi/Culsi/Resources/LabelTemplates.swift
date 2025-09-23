import CoreGraphics
import Foundation

struct LabelTemplate: Identifiable, Hashable, Codable {
    let id: String
    let name: String
    let columns: Int
    let rows: Int
    let labelSize: CGSize
    let pageSize: CGSize
    let horizontalSpacing: CGFloat
    let verticalSpacing: CGFloat

    var capacity: Int { rows * columns }
}

enum LabelTemplates {
    static let avery5160 = LabelTemplate(
        id: "avery-5160",
        name: "Avery 5160",
        columns: 3,
        rows: 10,
        labelSize: CGSize(width: 2.625 * 72, height: 1.0 * 72),
        pageSize: CGSize(width: 8.5 * 72, height: 11 * 72),
        horizontalSpacing: 0.125 * 72,
        verticalSpacing: 0.0 * 72
    )
}
