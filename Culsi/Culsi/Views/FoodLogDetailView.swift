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

    private let temperatureUnits: [MeasureUnit] = MeasureUnit.allCases.filter { $0 != .ea }
    private static let countdownFormatter: DateComponentsFormatter = {
        let f = DateComponentsFormatter()
        f.unitsStyle = .abbreviated
        f.allowedUnits = [.hour, .minute, .second]
        return f
    }()

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
                detailsSection
                holdingSection
                notesSection

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
            .onChange(of: input.policy) { newPolicy in
                guard case .create = mode else { return }
                if newPolicy == .tphc4h {
                    input.startedAt = Date()
                }
            }
        }
    }

    private var detailsSection: some View {
        Section("Details") {
            TextField("Name", text: $input.name)
            Stepper(value: $input.quantity, in: 0.0...999, step: 0.5) {
                Text("Quantity: \(input.quantity, specifier: "%.1f")")
            }
            TextField("Unit", text: $input.unit)
            Picker("Policy", selection: $input.policy) {
                ForEach(HoldPolicy.allCases) { policy in
                    Text(policy.title).tag(policy)
                }
            }
            .pickerStyle(.menu)
        }
    }

    private var holdingSection: some View {
        Section("Holding") {
            DatePicker("Start", selection: $input.startedAt, displayedComponents: [.date, .hourAndMinute])
            if input.policy == .tphc4h {
                HStack {
                    Label("Discard", systemImage: "timer")
                    Spacer()
                    Text(discardTimeString)
                        .foregroundStyle(input.isExpired ? Color.red : .primary)
                }
                if mode.isEditing {
                    TimelineView(.periodic(from: .now, by: 30)) { context in
                        let discardAt = input.startedAt.addingTimeInterval(4 * 60 * 60)
                        let remaining = max(0, discardAt.timeIntervalSince(context.date))
                        let message: String
                        if remaining > 0 {
                            let value = Self.countdownFormatter.string(from: remaining) ?? "--"
                            message = "Discard in \(value)"
                        } else {
                            message = "Expired"
                        }
                        Text(message)
                            .font(.caption)
                            .foregroundStyle(remaining > 0 ? Color.blue : .red)
                    }
                }
            }
            temperatureField
            Picker("Unit", selection: $input.tempUnit) {
                ForEach(temperatureUnits) { unit in
                    Text(unit.title).tag(unit)
                }
            }
            .pickerStyle(.segmented)
            TextField("Location", text: $input.location.orEmpty)
            TextField("Employee", text: $input.employee.orEmpty)
        }
    }

    private var notesSection: some View {
        Section("Notes") {
            TextField("Notes", text: $input.notes.orEmpty, axis: .vertical)
                .lineLimit(3...6)
        }
    }

    private var temperatureField: some View {
        TextField("Temperature", text: Binding(
            get: {
                if let value = input.measuredTemp {
                    return String(format: "%.0f", value)
                }
                return ""
            },
            set: { newValue in
                let trimmed = newValue.trimmingCharacters(in: .whitespacesAndNewlines)
                if trimmed.isEmpty {
                    input.measuredTemp = nil
                } else if let value = Double(trimmed) {
                    input.measuredTemp = value
                }
            }
        ))
        .keyboardType(.decimalPad)
    }

    private var discardTimeString: String {
        let discardDate = input.startedAt.addingTimeInterval(4 * 60 * 60)
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        formatter.dateStyle = .none
        return formatter.string(from: discardDate)
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

private extension FoodLogInput {
    var isExpired: Bool {
        policy == .tphc4h && Date() >= startedAt.addingTimeInterval(4 * 60 * 60)
    }
}

struct FoodLogDetailView_Previews: PreviewProvider {
    static var previews: some View {
        FoodLogDetailView(mode: .create) { _ in }
            .modelContainer(CulsiDatabase.shared.container)
    }
}
