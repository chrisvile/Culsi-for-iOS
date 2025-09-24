import SwiftUI
import CoreTransferable
import SwiftData

struct FoodLogListView: View {
    @StateObject private var viewModel = FoodLogViewModel()
    @State private var presentingCreate = false
    @State private var editingLog: FoodLog?
    private let exportService = ExportService()
    private let exportFormats: [ExportFormat] = [.csv, .json]

    var body: some View {
        NavigationStack {
            List {
                ForEach(viewModel.logs) { log in
                    Button {
                        editingLog = log
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(log.name)
                                    .font(.headline)
                                Text(log.date.formatted(date: .abbreviated, time: .omitted))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                if let notes = log.notes, !notes.isEmpty {
                                    Text(notes)
                                        .font(.footnote)
                                        .foregroundStyle(.secondary)
                                        .lineLimit(2)
                                }
                            }
                            Spacer()
                            Text(String(format: "%.2f %@", log.quantity, log.unit))
                                .font(.callout)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .buttonStyle(.plain)
                }
                .onDelete(perform: delete)
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Food Log")
            .searchable(text: $viewModel.searchText)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Menu {
                        Picker("Date Filter", selection: $viewModel.selectedFilter) {
                            ForEach(FoodLogViewModel.DateFilter.allCases) { filter in
                                Text(filter.title).tag(filter)
                            }
                        }
                    } label: {
                        Label("Filter", systemImage: "line.3.horizontal.decrease.circle")
                    }
                }
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Menu {
                        ForEach(exportFormats, id: \.self) { format in
                            if #available(iOS 16.0, *),
                               let payload: ExportPayload = (try? exportService.payload(for: viewModel.logs, format: format)) {
                                ShareLink(item: payload, preview: SharePreview("\(payload.filename).\(payload.format.fileExtension)")) {
                                    let icon = (format == .csv) ? "tablecells" : "curlybraces"
                                    Label("Share \(format.rawValue.uppercased())", systemImage: icon)
                                }
                            } else {
                                let icon = (format == .csv) ? "tablecells" : "curlybraces"
                                Label("Share \(format.rawValue.uppercased())", systemImage: icon)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    } label: {
                        Label("Export", systemImage: "square.and.arrow.up")
                    }
                    Button {
                        presentingCreate = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $presentingCreate) {
                FoodLogDetailView(mode: .create) { input in
                    viewModel.add(input)
                }
            }
            .sheet(item: $editingLog, onDismiss: { editingLog = nil }) { log in
                FoodLogDetailView(mode: .edit(log)) { input in
                    if let id = input.id { viewModel.update(id: id, with: input) }
                } onDelete: {
                    viewModel.delete(id: log.id)
                }
            }
        }
    }

    private func delete(at offsets: IndexSet) {
        for index in offsets {
            let log = viewModel.logs[index]
            viewModel.delete(id: log.id)
        }
    }
}

struct FoodLogListView_Previews: PreviewProvider {
    static var previews: some View {
        FoodLogListView()
            .modelContainer(CulsiDatabase.shared.container)
    }
}
