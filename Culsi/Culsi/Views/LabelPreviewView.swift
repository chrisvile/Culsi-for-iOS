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
                            Text(Converters.displayDateFormatter.string(from: log.date))
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
                        LabelPrinter.present(
                            placements: batchViewModel.sheetState.placements,
                            template: LabelTemplates.avery5160
                        )
                    }
                    Button("Reset") {
                        batchViewModel.reset()
                    }
                }
            }
            .sheet(isPresented: $showingPreview) {
                LabelPrintPreview(placements: batchViewModel.sheetState.placements)
            }
        }
    }

    private var labelGrid: some View {
        let template = LabelTemplates.avery5160
        return GeometryReader { geometry in
            let width = geometry.size.width
            let cellWidth = width / CGFloat(template.columns)
            LazyVGrid(columns: Array(repeating: GridItem(.fixed(cellWidth), spacing: 8), count: template.columns), spacing: 8) {
                ForEach(0..<template.capacity, id: \.self) { index in
                    let row = index / template.columns
                    let column = index % template.columns
                    let placement = batchViewModel.sheetState.placements.first(where: { $0.row == row && $0.column == column })
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
                    .frame(height: 70)
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
