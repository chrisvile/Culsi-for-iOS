#if canImport(UIKit)
import UIKit
#endif
import SwiftUI

struct LabelPreviewView: View {
    @StateObject private var batchViewModel = LabelBatchViewModel()
    @StateObject private var foodLogViewModel = FoodLogViewModel()
    @State private var showingPreview = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                labelGrid
                Button("Generate from Selected Logs") {
                    let selectedLogs = foodLogViewModel.logs.filter { batchViewModel.selectedLogs.contains($0.id) }
                    batchViewModel.generatePlacements(from: selectedLogs)
                }
                .buttonStyle(.borderedProminent)

                List(selection: $batchViewModel.selectedLogs) {
                    ForEach(foodLogViewModel.logs) { log in
                        VStack(alignment: .leading) {
                            Text(log.name)
                            Text(log.date.formatted(date: .abbreviated, time: .omitted))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                .environment(\.editMode, .constant(.active))
            }
            .padding()
            .navigationTitle("Labels")
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button("Preview") {
                        showingPreview = true
                    }
                    Button("Print") {
                        #if canImport(UIKit)
                        let renderer = LabelPrinter.from(
                            placements: batchViewModel.placements,
                            sheet: batchViewModel.sheetState
                        )
                        let controller = UIPrintInteractionController.shared
                        controller.printPageRenderer = renderer
                        controller.present(animated: true, completionHandler: nil)
                        #endif
                    }
                    Button("Reset") {
                        batchViewModel.reset()
                    }
                }
            }
            .sheet(isPresented: $showingPreview) {
                LabelPrintPreview(placements: batchViewModel.placements)
            }
        }
    }

    private var labelGrid: some View {
        let state = batchViewModel.sheetState
        return GeometryReader { geometry in
            let columns = max(state.columns, 1)
            let rows = max(state.rows, 1)
            let margin = CGFloat(state.marginInches * 72.0)
            let availableWidth = max(geometry.size.width - margin * 2.0, 0)
            let columnSpacing = CGFloat(8)
            let totalSpacing = columnSpacing * CGFloat(columns - 1)
            let preferredWidth = CGFloat(state.labelWidthInches * 72.0)
            let computedWidth = (availableWidth - totalSpacing) / CGFloat(columns)
            let cellWidth = preferredWidth > 0 ? min(max(computedWidth, 0), preferredWidth) : max(computedWidth, 0)
            let cellHeight = CGFloat(state.labelHeightInches * 72.0)
            LazyVGrid(
                columns: Array(repeating: GridItem(.fixed(max(cellWidth, 0)), spacing: columnSpacing), count: columns),
                spacing: columnSpacing
            ) {
                ForEach(0..<(columns * rows), id: \.self) { index in
                    let row = index / columns
                    let column = index % columns
                    let placement = batchViewModel.placements.first(where: { $0.row == row && $0.column == column })
                    ZStack {
                        RoundedRectangle(cornerRadius: 8)
                            .strokeBorder(.secondary)
                        if let placement {
                            Text(placement.text)
                                .font(.caption)
                                .padding(8)
                                .multilineTextAlignment(.center)
                        }
                    }
                    .frame(height: max(cellHeight, 44))
                }
            }
        }
        .frame(height: 320)
    }
}

private struct LabelPrintPreview: View {
    let placements: [AveryPlacement]

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                ForEach(placements) { placement in
                    RoundedRectangle(cornerRadius: 8)
                        .strokeBorder(.secondary)
                        .overlay(
                            Text(placement.text)
                                .padding()
                                .multilineTextAlignment(.center)
                        )
                        .frame(height: 72)
                }
            }
            .padding()
        }
        .presentationDetents([.medium, .large])
    }
}
