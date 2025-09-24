import Foundation
#if canImport(SwiftData)
import SwiftData
@available(iOS 17.0, *)
@Model
final class AverySheetState {
    @Attribute(.unique) var id: UUID
    var columns: Int
    var rows: Int
    var labelWidthInches: Double
    var labelHeightInches: Double
    var marginInches: Double
    init(id: UUID = UUID(), columns: Int = Int(3), rows: Int = Int(10),
         labelWidthInches: Double = Double(2.625), labelHeightInches: Double = Double(1.0),
         marginInches: Double = Double(0.125)) {
        self.id = id; self.columns = columns; self.rows = rows
        self.labelWidthInches = labelWidthInches; self.labelHeightInches = labelHeightInches
        self.marginInches = marginInches
    }
}
#else
struct AverySheetState: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var columns: Int = 3
    var rows: Int = 10
    var labelWidthInches: Double = 2.625
    var labelHeightInches: Double = 1.0
    var marginInches: Double = 0.125
}
#endif
