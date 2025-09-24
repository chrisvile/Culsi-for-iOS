import Foundation
import UIKit

final class LabelPrinter: UIPrintPageRenderer {
    override init() {
        super.init()
        let letter = CGRect(x: 0, y: 0, width: 612, height: 792) // 8.5x11 @ 72dpi
        setValue(letter, forKey: "paperRect")
        setValue(letter.insetBy(dx: 36, dy: 36), forKey: "printableRect")
    }
    override func drawPage(at pageIndex: Int, in printableRect: CGRect) {
        guard let ctx = UIGraphicsGetCurrentContext() else { return }
        ctx.saveGState(); defer { ctx.restoreGState() }
        let text = "Culsi Label Preview – Page \(pageIndex + 1)"
        let attrs: [NSAttributedString.Key: Any] = [.font: UIFont.systemFont(ofSize: 14, weight: .semibold)]
        text.draw(in: printableRect.insetBy(dx: 12, dy: 12), withAttributes: attrs)
    }
}
