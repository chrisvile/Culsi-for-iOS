import Combine
import Foundation

protocol FoodLogRepositoryType {
    var logsPublisher: AnyPublisher<[FoodLog], Never> { get }
    func setQuery(_ query: FoodLogQuery) async
    func refresh() async
    func add(_ new: FoodLogInput) async throws
    func update(_ log: FoodLog) async throws
    func delete(id: UUID) async throws
}

final class FoodLogRepository: FoodLogRepositoryType {
    private let store: FoodLogStore
    private let subject = CurrentValueSubject<[FoodLog], Never>([])
    private var query = FoodLogQuery()

    var logsPublisher: AnyPublisher<[FoodLog], Never> {
        subject.eraseToAnyPublisher()
    }

    init(store: FoodLogStore = FoodLogStore()) {
        self.store = store
        Task { await refresh() }
    }

    func setQuery(_ query: FoodLogQuery) async {
        self.query = query
        await refresh()
    }

    func refresh() async {
        do {
            let logs = try await store.fetchLogs(matching: query)
            subject.send(logs)
        } catch {
            #if DEBUG
            print("Failed to fetch logs: \(error)")
            #endif
            subject.send([])
        }
    }

    func add(_ new: FoodLogInput) async throws {
        _ = try await store.insert(new)
        await refresh()
    }

    func update(_ log: FoodLog) async throws {
        let input = FoodLogInput(log: log)
        guard let id = input.id else { return }
        _ = try await store.update(id: id, with: input)
        await refresh()
    }

    func delete(id: UUID) async throws {
        try await store.delete(id: id)
        await refresh()
    }
}
