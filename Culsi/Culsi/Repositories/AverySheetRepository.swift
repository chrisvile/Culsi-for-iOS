import Combine
import Foundation

protocol AverySheetRepositoryType {
    var statePublisher: AnyPublisher<AverySheetState, Never> { get }
    func load() async
    func save(_ state: AverySheetState) async
    func reset() async
}

@MainActor
final class AverySheetRepository: AverySheetRepositoryType {
    private let store: AverySheetStateStore
    private let subject: CurrentValueSubject<AverySheetState, Never>

    init(store: AverySheetStateStore = .shared) {
        self.store = store
        self.subject = CurrentValueSubject(AverySheetState())
    }

    var statePublisher: AnyPublisher<AverySheetState, Never> {
        subject.eraseToAnyPublisher()
    }

    func load() async {
        do {
            if let existing = try store.fetch().first {
                subject.send(existing)
            } else {
                let state = AverySheetState()
                try store.upsert(state)
                subject.send(state)
            }
        } catch {
            #if DEBUG
            print("Failed to load Avery sheet state: \(error)")
            #endif
        }
    }

    func save(_ state: AverySheetState) async {
        do {
            try store.upsert(state)
            subject.send(state)
        } catch {
            #if DEBUG
            print("Failed to save Avery sheet state: \(error)")
            #endif
        }
    }

    func reset() async {
        do {
            let states = try store.fetch()
            for state in states {
                try store.delete(state)
            }
            let fresh = AverySheetState()
            try store.upsert(fresh)
            subject.send(fresh)
        } catch {
            #if DEBUG
            print("Failed to reset Avery sheet state: \(error)")
            #endif
        }
    }
}
