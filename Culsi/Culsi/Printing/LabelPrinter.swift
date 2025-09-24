import Foundation
import UIKit

// Minimal info needed to render a label. If you already have AveryPlacement, adapt initializer to use it.
struct SimplePlacement {
    let title: String
    let subtitle: String?
}

final class LabelPrinter: UIPrintPageRenderer {
    private let placements: [SimplePlacement]
    private let columns: Int
    private let rows: Int

    private var labelsPerPage: Int { max(1, columns * rows) }
    private var totalPages: Int {
        let count = placements.count
        return max(1, Int(ceil(Double(count) / Double(labelsPerPage))))
    }

    init(placements: [SimplePlacement], columns: Int, rows: Int) {
        self.placements = placements
        self.columns = max(1, columns)
        self.rows = max(1, rows)
        super.init()
        // US Letter
        let letter = CGRect(x: 0, y: 0, width: 612, height: 792) // 8.5x11 @ 72dpi
        setValue(letter, forKey: "paperRect")
        setValue(letter.insetBy(dx: 36, dy: 36), forKey: "printableRect")
    }

    override var numberOfPages: Int { totalPages }

    override func drawPage(at pageIndex: Int, in printableRect: CGRect) {
        guard let ctx = UIGraphicsGetCurrentContext() else { return }
        ctx.saveGState(); defer { ctx.restoreGState() }

        let cellW = printableRect.width / CGFloat(columns)
        let cellH = printableRect.height / CGFloat(rows)
        let start = pageIndex * labelsPerPage
        let end = min(start + labelsPerPage, placements.count)

        let titleAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 12, weight: .semibold),
            .foregroundColor: UIColor.black
        ]
        let subAttrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 10),
            .foregroundColor: UIColor.darkGray
        ]

        for r in 0..<rows {
            for c in 0..<columns {
                let idx = start + r * columns + c
                let cell = CGRect(
                    x: printableRect.minX + CGFloat(c) * cellW,
                    y: printableRect.minY + CGFloat(r) * cellH,
                    width: cellW, height: cellH
                ).insetBy(dx: 6, dy: 6)

                // Draw cell border (light)
                UIColor(white: 0.85, alpha: 1).setStroke()
                UIBezierPath(rect: cell).stroke()

                guard idx < end else { continue }
                let placement = placements[idx]

                // Draw text
                placement.title.draw(in: cell.insetBy(dx: 4, dy: 4), withAttributes: titleAttrs)
                if let subtitle = placement.subtitle, !subtitle.isEmpty {
                    let subRect = cell.insetBy(dx: 4, dy: 24)
                    subtitle.draw(in: subRect, withAttributes: subAttrs)
                }
            }
        }
    }
}

extension LabelPrinter {
    // Bridge from your real models to SimplePlacement + grid sizes.
    static func from(placements: [AveryPlacement], sheet: AverySheetState) -> LabelPrinter {
        // Map AveryPlacement fields to a simple title/subtitle.
        let mapped: [SimplePlacement] = placements.map { placement in
            let parts = placement.text.components(separatedBy: .newlines)
            let rawTitle = parts.first?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "Label"
            let subtitleText = parts.dropFirst().joined(separator: "\n").trimmingCharacters(in: .whitespacesAndNewlines)
            let title = rawTitle.isEmpty ? "Label" : rawTitle
            let subtitle = subtitleText.isEmpty ? nil : subtitleText
            return SimplePlacement(title: title, subtitle: subtitle)
        }
        return LabelPrinter(placements: mapped, columns: sheet.columns, rows: sheet.rows)
    }
}

extension LabelPrinter {
    static func from(foodLogs: [FoodLog]) -> LabelPrinter {
        let df = DateFormatter()
        df.timeStyle = .short
        let placements = foodLogs.map { log in
            let subtitle: String = {
                switch log.policy {
                case .tphc4h:
                    return "Start: \(df.string(from: log.resolvedStartedAt))  Discard: \(df.string(from: log.expiresAt))"
                case .hotHold, .coldHold:
                    if let t = log.measuredTemp {
                        let unit = log.tempUnit == .f ? "°F" : "°C"
                        return "Temp: \(Int(t.rounded()))\(unit)"
                    }
                    return ""
                }
            }()
            return SimplePlacement(title: log.name, subtitle: subtitle)
        }
        return LabelPrinter(placements: placements, columns: 3, rows: 10)
    }
}
