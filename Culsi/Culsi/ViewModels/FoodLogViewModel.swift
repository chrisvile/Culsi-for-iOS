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
    @Published var searchText: String = ""
    @Published var selectedFilter: DateFilter = .all

    private let repository: FoodLogRepositoryType
    private var cancellables = Set<AnyCancellable>()

    init(repository: FoodLogRepositoryType = FoodLogRepository()) {
        self.repository = repository

        repository.logsPublisher
            .receive(on: RunLoop.main)
            .sink { [weak self] logs in
                self?.logs = logs
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

        Task { await repository.refresh() }
    }

    func add(_ input: FoodLogInput) {
        Task {
            try? await repository.add(input)
        }
    }

    func update(id: UUID, with input: FoodLogInput) {
        let updated = FoodLog(
            id: id,
            name: input.name,
            date: input.date,
            quantity: input.quantity,
            unit: input.unit,
            notes: input.notes
        )
        Task {
            try? await repository.update(updated)
        }
    }

    func delete(id: UUID) {
        Task {
            try? await repository.delete(id: id)
        }
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
}
