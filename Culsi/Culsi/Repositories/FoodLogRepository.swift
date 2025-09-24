import Combine
import Foundation

protocol FoodLogRepositoryType {
    var logsPublisher: AnyPublisher<[FoodLog], Never> { get }
    var activeTPHCPublisher: AnyPublisher<[FoodLog], Never> { get }
    var expiredTPHCPublisher: AnyPublisher<[FoodLog], Never> { get }
    func setQuery(_ query: FoodLogQuery) async
    func refresh() async
    func add(_ new: FoodLogInput) async throws
    func update(_ log: FoodLog) async throws
    func delete(id: UUID) async throws
    func fetchActiveTPHC() async -> [FoodLog]
    func fetchExpiredTPHC() async -> [FoodLog]
    func updateTemperature(id: UUID, measuredTemp: Double?, unit: MeasureUnit) async throws
}

final class FoodLogRepository: FoodLogRepositoryType {
    private let store: FoodLogStore
    private let subject = CurrentValueSubject<[FoodLog], Never>([])
    private let activeTPHCSubject = CurrentValueSubject<[FoodLog], Never>([])
    private let expiredTPHCSubject = CurrentValueSubject<[FoodLog], Never>([])
    private var query = FoodLogQuery()

    var logsPublisher: AnyPublisher<[FoodLog], Never> {
        subject.eraseToAnyPublisher()
    }

    var activeTPHCPublisher: AnyPublisher<[FoodLog], Never> {
        activeTPHCSubject.eraseToAnyPublisher()
    }

    var expiredTPHCPublisher: AnyPublisher<[FoodLog], Never> {
        expiredTPHCSubject.eraseToAnyPublisher()
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
            try await updateTPHCStates()
        } catch {
            #if DEBUG
            print("Failed to fetch logs: \(error)")
            #endif
            subject.send([])
            activeTPHCSubject.send([])
            expiredTPHCSubject.send([])
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

    func fetchActiveTPHC() async -> [FoodLog] {
        (try? await store.fetchActiveTPHC()) ?? []
    }

    func fetchExpiredTPHC() async -> [FoodLog] {
        (try? await store.fetchExpiredTPHC()) ?? []
    }

    func updateTemperature(id: UUID, measuredTemp: Double?, unit: MeasureUnit) async throws {
        _ = try await store.updateTemperature(id: id, measuredTemp: measuredTemp, unit: unit)
        await refresh()
    }

    private func updateTPHCStates() async throws {
        let active = try await store.fetchActiveTPHC()
        let expired = try await store.fetchExpiredTPHC()
        activeTPHCSubject.send(active)
        expiredTPHCSubject.send(expired)
    }
}
