import SwiftUI

struct ItemCatalogView: View {
    @StateObject private var viewModel = ItemCatalogViewModel()
    @State private var presentingForm = false
    @State private var editingItem: CatalogItem?

    var body: some View {
        NavigationStack {
            List {
                ForEach(viewModel.items) { item in
                    Button {
                        editingItem = item
                    } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.name)
                                .font(.headline)
                            if let notes = item.notes, !notes.isEmpty {
                                Text(notes)
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                            Text("Default unit: \(item.defaultUnit)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .buttonStyle(.plain)
                }
                .onDelete(perform: delete)
            }
            .navigationTitle("Catalog")
            .searchable(text: $viewModel.searchText)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { presentingForm = true } label: { Image(systemName: "plus") }
                }
            }
            .sheet(isPresented: $presentingForm) {
                CatalogItemFormView(mode: .create) { input in
                    viewModel.addOrUpdate(input)
                }
            }
            .sheet(item: $editingItem, onDismiss: { editingItem = nil }) { item in
                CatalogItemFormView(mode: .edit(item)) { input in
                    viewModel.addOrUpdate(input)
                } onDelete: {
                    viewModel.delete(id: item.id)
                }
            }
        }
    }

    private func delete(at offsets: IndexSet) {
        for index in offsets {
            let item = viewModel.items[index]
            viewModel.delete(id: item.id)
        }
    }
}

private struct CatalogItemFormView: View {
    enum Mode {
        case create
        case edit(CatalogItem)
    }

    let mode: Mode
    var onSave: (CatalogItemInput) -> Void
    var onDelete: (() -> Void)?
    @Environment(\.dismiss) private var dismiss
    @State private var input: CatalogItemInput

    init(mode: Mode, onSave: @escaping (CatalogItemInput) -> Void, onDelete: (() -> Void)? = nil) {
        self.mode = mode
        self.onSave = onSave
        self.onDelete = onDelete
        switch mode {
        case .create:
            _input = State(initialValue: CatalogItemInput())
        case .edit(let item):
            _input = State(initialValue: CatalogItemInput(item: item))
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Item") {
                    TextField("Name", text: $input.name)
                    TextField("Unit", text: $input.defaultUnit)
                    TextField("Notes", text: Binding($input.notes, replacingNilWith: ""), axis: .vertical)
                        .lineLimit(2...4)
                }
                if mode.isEditing, let onDelete {
                    Section {
                        Button(role: .destructive) {
                            onDelete()
                            dismiss()
                        } label: {
                            Label("Delete Item", systemImage: "trash")
                        }
                    }
                }
            }
            .navigationTitle(mode.title)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        if input.id == nil, case .edit(let item) = mode { input.id = item.id }
                        onSave(input)
                        dismiss()
                    }
                    .disabled(input.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}

private extension CatalogItemFormView.Mode {
    var title: String {
        switch self {
        case .create: return "New Item"
        case .edit: return "Edit Item"
        }
    }

    var isEditing: Bool {
        if case .edit = self { return true }
        return false
    }
}

extension CatalogItem: Identifiable {}

struct ItemCatalogView_Previews: PreviewProvider {
    static var previews: some View {
        ItemCatalogView()
            .modelContainer(CulsiDatabase.shared.container)
    }
}
