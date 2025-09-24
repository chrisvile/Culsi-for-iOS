import Combine
import Foundation

@MainActor
final class FoodLogViewModel: ObservableObject {
    enum DateFilter: CaseIterable, Identifiable {
        case all
        case today
        case lastSevenDays
        case lastThirtyDays

        var id: String { title }

        var title: String {
            switch self {
            case .all: return "All"
            case .today: return "Today"
            case .lastSevenDays: return "7 Days"
            case .lastThirtyDays: return "30 Days"
            }
        }

        var interval: DateInterval? {
            let now = Date()
            let calendar = Calendar.current
            switch self {
            case .all:
                return nil
            case .today:
                let startOfDay = calendar.startOfDay(for: now)
                return DateInterval(start: startOfDay, end: now)
            case .lastSevenDays:
                let start = calendar.date(byAdding: .day, value: -6, to: calendar.startOfDay(for: now)) ?? now
                return DateInterval(start: start, end: now)
            case .lastThirtyDays:
                let start = calendar.date(byAdding: .day, value: -29, to: calendar.startOfDay(for: now)) ?? now
                return DateInterval(start: start, end: now)
            }
        }
    }

    @Published private(set) var logs: [FoodLog] = []
    @Published private(set) var displayedLogs: [FoodLog] = []
    @Published var searchText: String = ""
    @Published var selectedFilter: DateFilter = .all
    @Published var filter: HoldPolicy? = nil
    @Published var showExpired = false
    @Published var selectedLogIDs: Set<UUID> = []

    private let repository: FoodLogRepositoryType
    private let tphcService: TPHCService
    private var cancellables = Set<AnyCancellable>()

    init(repository: FoodLogRepositoryType = FoodLogRepository()) {
        self.repository = repository
        self.tphcService = TPHCService(repository: repository)

        repository.logsPublisher
            .receive(on: RunLoop.main)
            .sink { [weak self] logs in
                guard let self else { return }
                self.logs = logs
                let validIDs = Set(logs.map(\.id))
                self.selectedLogIDs = self.selectedLogIDs.intersection(validIDs)
            }
            .store(in: &cancellables)

        $searchText
            .removeDuplicates()
            .debounce(for: .milliseconds(250), scheduler: RunLoop.main)
            .sink { [weak self] _ in
                Task { await self?.applyFilters() }
            }
            .store(in: &cancellables)

        $selectedFilter
            .removeDuplicates()
            .sink { [weak self] _ in
                Task { await self?.applyFilters() }
            }
            .store(in: &cancellables)

        Publishers.CombineLatest3($logs, $filter, $showExpired)
            .map { logs, filter, showExpired in
                FoodLogViewModel.filterLogs(logs, policy: filter, showExpired: showExpired)
            }
            .receive(on: RunLoop.main)
            .assign(to: & $displayedLogs)

        Task { await repository.refresh() }
    }

    func add(_ input: FoodLogInput) {
        Task {
            var payload = input
            payload.date = input.startedAt
            try? await repository.add(payload)
        }
    }

    func update(id: UUID, with input: FoodLogInput) {
        Task {
            var payload = input
            payload.id = id
            payload.date = input.startedAt
            let updated = FoodLog(input: payload)
            try? await repository.update(updated)
        }
    }

    func delete(id: UUID) {
        Task {
            try? await repository.delete(id: id)
        }
    }

    func updateTemperature(id: UUID, measuredTemp: Double?, unit: MeasureUnit) {
        Task {
            try? await repository.updateTemperature(id: id, measuredTemp: measuredTemp, unit: unit)
        }
    }

    func onAppear() {
        tphcService.start()
    }

    func onDisappear() {
        tphcService.stop()
    }

    var logsSelection: [FoodLog] {
        logs.filter { selectedLogIDs.contains($0.id) }
    }

    private func applyFilters() async {
        var query = FoodLogQuery()
        if !searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            query.searchText = searchText
        }
        if let interval = selectedFilter.interval {
            query.startDate = interval.start
            query.endDate = interval.end
        }
        await repository.setQuery(query)
    }

    private static func filterLogs(_ logs: [FoodLog], policy: HoldPolicy?, showExpired: Bool) -> [FoodLog] {
        if showExpired {
            return logs.filter { $0.policy == .tphc4h && $0.isExpired }
        }
        guard let policy else { return logs }
        switch policy {
        case .tphc4h:
            return logs.filter { $0.policy == .tphc4h && !$0.isExpired }
        case .hotHold, .coldHold:
            return logs.filter { $0.policy == policy }
        }
    }
}
