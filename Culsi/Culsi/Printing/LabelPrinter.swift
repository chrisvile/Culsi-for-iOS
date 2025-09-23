import UIKit

final class LabelPrinter {
    func renderer(for placements: [AveryPlacement], template: LabelTemplate) -> UIPrintPageRenderer {
        LabelPageRenderer(placements: placements, template: template)
    }

    func print(placements: [AveryPlacement], template: LabelTemplate) {
        #if os(iOS)
        let controller = UIPrintInteractionController.shared
        controller.printPageRenderer = renderer(for: placements, template: template)
        controller.present(animated: true, completionHandler: nil)
        #endif
    }
}

private final class LabelPageRenderer: UIPrintPageRenderer {
    private let template: LabelTemplate
    private let placementMap: [String: AveryPlacement]

    init(placements: [AveryPlacement], template: LabelTemplate) {
        self.template = template
        self.placementMap = Dictionary(uniqueKeysWithValues: placements.map { ("\($0.row)-\($0.column)", $0) })
        super.init()
    }

    override var paperRect: CGRect {
        CGRect(origin: .zero, size: template.pageSize)
    }

    override var printableRect: CGRect {
        paperRect.insetBy(dx: 18, dy: 36)
    }

    override func numberOfPages() -> Int {
        1
    }

    override func drawContentForPage(at pageIndex: Int, in printableRect: CGRect) {
        guard pageIndex == 0 else { return }
        let labelWidth = template.labelSize.width
        let labelHeight = template.labelSize.height
        let totalWidth = CGFloat(template.columns) * labelWidth + CGFloat(template.columns - 1) * template.horizontalSpacing
        let totalHeight = CGFloat(template.rows) * labelHeight + CGFloat(template.rows - 1) * template.verticalSpacing

        let originX = printableRect.midX - totalWidth / 2
        let originY = printableRect.minY + (printableRect.height - totalHeight) / 2

        for row in 0..<template.rows {
            for column in 0..<template.columns {
                let x = originX + CGFloat(column) * (labelWidth + template.horizontalSpacing)
                let y = originY + CGFloat(row) * (labelHeight + template.verticalSpacing)
                let frame = CGRect(x: x, y: y, width: labelWidth, height: labelHeight)
                drawLabel(in: frame, row: row, column: column)
            }
        }
    }

    private func drawLabel(in rect: CGRect, row: Int, column: Int) {
        let path = UIBezierPath(roundedRect: rect, cornerRadius: 8)
        UIColor.black.setStroke()
        path.lineWidth = 0.5
        path.stroke()

        if let placement = placementMap["\(row)-\(column)"] {
            let paragraph = NSMutableParagraphStyle()
            paragraph.alignment = .center
            let attributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 12),
                .paragraphStyle: paragraph
            ]
            let textRect = rect.insetBy(dx: 6, dy: 6)
            placement.text.draw(in: textRect, withAttributes: attributes)
        }
    }
}
