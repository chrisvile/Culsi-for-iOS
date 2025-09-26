import SwiftUI

enum FoodLogEditMode {
    case add
    case edit(FoodLog)
}

struct FoodLogEditorCard: View {
    let mode: FoodLogEditMode
    var onSave: (FoodLogInput) -> Void
    var onCancel: () -> Void = {}
    var onDelete: (() -> Void)? = nil

    struct FoodLogInput: Equatable {
        var name: String = ""
        var policy: HoldPolicy = .tphc4h
        var startedAt: Date = .now
        var measuredTemp: Double? = nil
        var tempUnit: MeasureUnit = .f
        var location: String? = nil
        var employee: String? = nil
        var notes: String? = nil
    }

    @State private var input: FoodLogInput
    @FocusState private var focusedField: Field?
    private enum Field { case name, temp, location, employee, notes }

    init(mode: FoodLogEditMode, onSave: @escaping (FoodLogInput) -> Void, onCancel: @escaping () -> Void = {}, onDelete: (() -> Void)? = nil) {
        self.mode = mode
        self.onSave = onSave
        self.onCancel = onCancel
        self.onDelete = onDelete
        switch mode {
        case .add:
            _input = State(initialValue: .init())
        case .edit(let log):
            _input = State(initialValue: .init(
                name: log.name,
                policy: log.policy,
                startedAt: log.resolvedStartedAt,
                measuredTemp: log.measuredTemp,
                tempUnit: log.tempUnit,
                location: log.location,
                employee: log.employee,
                notes: log.notes
            ))
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label(modeTitle, systemImage: modeIcon)
                    .font(.headline)
                Spacer()
                if let onDelete {
                    Button(role: .destructive) { onDelete() } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
                Button("Cancel", role: .cancel, action: onCancel)
                Button("Save") { onSave(input) }
                    .buttonStyle(.borderedProminent)
                    .disabled(!isValid)
            }

            TextField("Item name", text: $input.name)
                .textInputAutocapitalization(.words)
                .focused($focusedField, equals: .name)

            HStack {
                Picker("Policy", selection: $input.policy) {
                    ForEach(HoldPolicy.allCases, id: \.self) { policy in
                        Text(label(for: policy)).tag(policy)
                    }
                }
                .pickerStyle(.segmented)

                DatePicker("Start", selection: $input.startedAt, displayedComponents: [.date, .hourAndMinute])
                    .labelsHidden()
            }

            if input.policy == .tphc4h {
                HStack {
                    Label("Discard", systemImage: "timer")
                    Spacer()
                    Text(discardTimeString)
                        .foregroundStyle(isExpired ? .red : .primary)
                }
                TPHCCountdownView(startedAt: input.startedAt)
            }

            HStack {
                TextField("Temp", value: $input.measuredTemp, format: .number.precision(.fractionLength(0)))
                    .keyboardType(.numberPad)
                    .focused($focusedField, equals: .temp)

                Picker("Unit", selection: $input.tempUnit) {
                    ForEach(MeasureUnit.allCases, id: \.self) { unit in
                        Text(unit == .f ? "°F" : unit == .c ? "°C" : "ea").tag(unit)
                    }
                }
                .pickerStyle(.segmented)
            }

            HStack {
                TextField("Location", text: $input.location.orEmpty)
                    .focused($focusedField, equals: .location)
                TextField("Employee", text: $input.employee.orEmpty)
                    .focused($focusedField, equals: .employee)
            }

            TextField("Notes", text: $input.notes.orEmpty, axis: .vertical)
                .lineLimit(1...3)
                .focused($focusedField, equals: .notes)
        }
        .padding(14)
        .background(.ultraThickMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16)
                .strokeBorder(Color.secondary.opacity(0.2))
        }
        .shadow(color: .black.opacity(0.06), radius: 12, y: 4)
        .animation(.snappy, value: input)
    }

    private var isValid: Bool {
        !input.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private var isExpired: Bool {
        Date() >= input.startedAt.addingTimeInterval(4 * 60 * 60)
    }

    private var discardTimeString: String {
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        formatter.dateStyle = .none
        return formatter.string(from: input.startedAt.addingTimeInterval(4 * 60 * 60))
    }

    private var modeTitle: String {
        switch mode {
        case .add:
            return "Add Item"
        case .edit:
            return "Edit Item"
        }
    }

    private var modeIcon: String {
        switch mode {
        case .add:
            return "plus.square"
        case .edit:
            return "pencil.and.outline"
        }
    }

    private func label(for policy: HoldPolicy) -> String {
        switch policy {
        case .hotHold:
            return "Hot"
        case .coldHold:
            return "Cold"
        case .tphc4h:
            return "TPHC 4h"
        }
    }
}
