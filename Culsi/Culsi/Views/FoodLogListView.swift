import SwiftUI
import CoreTransferable
import SwiftData

struct FoodLogListView: View {
    private enum PolicyFilter: String, CaseIterable, Identifiable {
        case all
        case tphc
        case hot
        case cold
        case expired

        var id: String { rawValue }

        var title: String {
            switch self {
            case .all: return "All"
            case .tphc: return "TPHC"
            case .hot: return "Hot Hold"
            case .cold: return "Cold Hold"
            case .expired: return "Expired"
            }
        }
    }

    @StateObject private var viewModel = FoodLogViewModel()
    @State private var presentingCreate = false
    @State private var editingLog: FoodLog?
    private let exportService = ExportService()
    private let exportFormats: [ExportFormat] = [.csv, .json]

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Picker("Policy Filter", selection: policyFilterBinding) {
                        ForEach(PolicyFilter.allCases) { filter in
                            Text(filter.title).tag(filter)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                Section {
                    ForEach(viewModel.displayedLogs) { log in
                        Button {
                            editingLog = log
                        } label: {
                            FoodLogRow(log: log)
                        }
                        .buttonStyle(.plain)
                    }
                    .onDelete(perform: delete)
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Food Log")
            .searchable(text: $viewModel.searchText)
            .onAppear { viewModel.onAppear() }
            .onDisappear { viewModel.onDisappear() }
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
            let log = viewModel.displayedLogs[index]
            viewModel.delete(id: log.id)
        }
    }

    private var policyFilterBinding: Binding<PolicyFilter> {
        Binding<PolicyFilter>(
            get: {
                if viewModel.showExpired { return .expired }
                switch viewModel.filter {
                case .none:
                    return .all
                case .some(.tphc4h):
                    return .tphc
                case .some(.hotHold):
                    return .hot
                case .some(.coldHold):
                    return .cold
                }
            },
            set: { selection in
                switch selection {
                case .all:
                    viewModel.filter = nil
                    viewModel.showExpired = false
                case .tphc:
                    viewModel.filter = .tphc4h
                    viewModel.showExpired = false
                case .hot:
                    viewModel.filter = .hotHold
                    viewModel.showExpired = false
                case .cold:
                    viewModel.filter = .coldHold
                    viewModel.showExpired = false
                case .expired:
                    viewModel.filter = .tphc4h
                    viewModel.showExpired = true
                }
            }
        )
    }
}

struct FoodLogListView_Previews: PreviewProvider {
    static var previews: some View {
        FoodLogListView()
            .modelContainer(CulsiDatabase.shared.container)
    }
}

private struct FoodLogRow: View {
    let log: FoodLog

    private static let countdownFormatter: DateComponentsFormatter = {
        let formatter = DateComponentsFormatter()
        formatter.allowedUnits = [.hour, .minute]
        formatter.unitsStyle = .abbreviated
        return formatter
    }()

    private static let startedFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        return formatter
    }()

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: log.policy.systemImage)
                .font(.title3)
                .foregroundStyle(iconColor)
                .padding(.top, 4)

            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 8) {
                    Text(log.name)
                        .font(.headline)
                        .foregroundStyle(.primary)
                    if log.policy == .tphc4h, log.isExpired {
                        Text("Expired")
                            .font(.caption2)
                            .fontWeight(.semibold)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.red.opacity(0.15))
                            .foregroundStyle(.red)
                            .clipShape(Capsule())
                    }
                }

                Text(log.policy.title)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                Text("Start: \(Self.startedFormatter.string(from: log.resolvedStartedAt))")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                if log.policy == .tphc4h {
                    Text(tphcCountdownText)
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundStyle(log.isExpired ? Color.red : .blue)
                } else if let measured = log.measuredTemp {
                    Text("Temp: \(Int(measured.rounded()))\(log.tempUnit.title)")
                        .font(.caption)
                        .foregroundStyle(.primary)
                }

                if let location = log.location, !location.isEmpty {
                    Text("Location: \(location)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                if let employee = log.employee, !employee.isEmpty {
                    Text("Employee: \(employee)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                if let notes = log.notes, !notes.isEmpty {
                    Text(notes)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
            }
        }
        .padding(.vertical, 4)
    }

    private var iconColor: Color {
        if log.policy == .tphc4h && log.isExpired { return .red }
        switch log.policy {
        case .hotHold:
            return .orange
        case .coldHold:
            return .blue
        case .tphc4h:
            return .green
        }
    }

    private var tphcCountdownText: String {
        let remaining = max(0, log.expiresAt.timeIntervalSinceNow)
        if remaining == 0 || log.isExpired {
            let discardTime = Self.startedFormatter.string(from: log.expiresAt)
            return "Discard at: \(discardTime)"
        }
        let formatted = Self.countdownFormatter.string(from: remaining) ?? "--"
        return "Discard in: \(formatted)"
    }
}
