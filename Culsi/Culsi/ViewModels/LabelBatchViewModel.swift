import Foundation
import Combine

@MainActor
final class LabelBatchViewModel: ObservableObject {
    private let sheetRepository: AverySheetRepositoryType
    @Published var sheetState: AverySheetState
    private var cancellables = Set<AnyCancellable>()
    @Published private(set) var placements: [AveryPlacement] = []
    @Published var selectedLogs: Set<UUID> = []

    init(sheetRepository: AverySheetRepositoryType? = nil) {
        // Instantiate default on the main actor (safe), avoiding default-arg evaluation off-main.
        self.sheetRepository = sheetRepository ?? AverySheetRepository()
        self.sheetState = AverySheetState()

        self.sheetRepository.statePublisher
            .receive(on: RunLoop.main)
            .sink { [weak self] state in
                self?.sheetState = state
            }
            .store(in: &cancellables)

        Task { await self.sheetRepository.load() }
    }

    func generatePlacements(from logs: [FoodLog]) {
        let columns = max(sheetState.columns, 1)
        let rows = max(sheetState.rows, 1)
        let capacity = columns * rows
        var nextPlacements: [AveryPlacement] = []
        for (index, log) in logs.enumerated() where index < capacity {
            let row = index / columns
            let column = index % columns
            let text = "\(log.name)\n\(log.date.formatted(date: .abbreviated, time: .omitted))"
            nextPlacements.append(AveryPlacement(row: row, column: column, text: text))
        }
        placements = nextPlacements
    }

    func reset() {
        placements.removeAll()
        Task { await sheetRepository.reset() }
    }
}
