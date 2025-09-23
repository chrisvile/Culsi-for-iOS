import Combine
import Foundation

protocol AverySheetRepositoryType {
    var statePublisher: AnyPublisher<AverySheetState, Never> { get }
    func load() async
    func updatePlacements(_ placements: [AveryPlacement]) async
    func reset() async
}

final class AverySheetRepository: AverySheetRepositoryType {
    private let store: AverySheetStateStore
    private let subject = CurrentValueSubject<AverySheetState, Never>(AverySheetState(templateIdentifier: LabelTemplates.avery5160.id))

    var statePublisher: AnyPublisher<AverySheetState, Never> {
        subject.eraseToAnyPublisher()
    }

    init(store: AverySheetStateStore = AverySheetStateStore()) {
        self.store = store
        Task { await load() }
    }

    func load() async {
        do {
            let state = try await store.loadCurrent(templateIdentifier: LabelTemplates.avery5160.id)
            subject.send(state)
        } catch {
            #if DEBUG
            print("Failed to load sheet state: \(error)")
            #endif
        }
    }

    func updatePlacements(_ placements: [AveryPlacement]) async {
        do {
            var state = try await store.loadCurrent(templateIdentifier: LabelTemplates.avery5160.id)
            state.placements = placements
            try await store.save(state)
            subject.send(state)
        } catch {
            #if DEBUG
            print("Failed to update sheet state: \(error)")
            #endif
        }
    }

    func reset() async {
        do {
            let state = try await store.reset(templateIdentifier: LabelTemplates.avery5160.id)
            subject.send(state)
        } catch {
            #if DEBUG
            print("Failed to reset sheet state: \(error)")
            #endif
        }
    }
}
