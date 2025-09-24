import SwiftUI
import SwiftData

struct FoodLogDetailView: View {
    enum Mode {
        case create
        case edit(FoodLog)
    }

    let mode: Mode
    var onSave: (FoodLogInput) -> Void
    var onDelete: (() -> Void)?
    @Environment(\.dismiss) private var dismiss
    @State private var input: FoodLogInput

    init(mode: Mode, onSave: @escaping (FoodLogInput) -> Void, onDelete: (() -> Void)? = nil) {
        self.mode = mode
        self.onSave = onSave
        self.onDelete = onDelete
        switch mode {
        case .create:
            _input = State(initialValue: FoodLogInput())
        case .edit(let log):
            _input = State(initialValue: FoodLogInput(log: log))
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Details") {
                    TextField("Name", text: $input.name)
                    DatePicker("Date", selection: $input.date, displayedComponents: .date)
                    Stepper(value: $input.quantity, in: 0.0...999, step: 0.5) {
                        Text("Quantity: \(input.quantity, specifier: "%.1f")")
                    }
                    TextField("Unit", text: $input.unit)
                    TextField("Notes", text: $input.notes.orEmpty, axis: .vertical)
                        .lineLimit(2...4)
                }
                if mode.isEditing, let onDelete {
                    Section {
                        Button(role: .destructive) {
                            onDelete()
                            dismiss()
                        } label: {
                            Label("Delete Entry", systemImage: "trash")
                        }
                    }
                }
            }
            .navigationTitle(mode.title)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        if input.id == nil, case .edit(let log) = mode {
                            input.id = log.id
                        }
                        onSave(input)
                        dismiss()
                    }
                    .disabled(input.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}

private extension FoodLogDetailView.Mode {
    var title: String {
        switch self {
        case .create: return "New Entry"
        case .edit: return "Edit Entry"
        }
    }

    var isEditing: Bool {
        if case .edit = self { return true }
        return false
    }
}

struct FoodLogDetailView_Previews: PreviewProvider {
    static var previews: some View {
        FoodLogDetailView(mode: .create) { _ in }
            .modelContainer(CulsiDatabase.shared.container)
    }
}
