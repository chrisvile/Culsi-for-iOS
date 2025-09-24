import Combine
import Foundation

@MainActor
final class LabelBatchViewModel: ObservableObject {
    @Published private(set) var sheetState: AverySheetState
    @Published private(set) var placements: [AveryPlacement] = []
    @Published var selectedLogs: Set<UUID> = []

    private let sheetRepository: AverySheetRepositoryType
    private var cancellables = Set<AnyCancellable>()

    init(sheetRepository: AverySheetRepositoryType = AverySheetRepository()) {
        self.sheetRepository = sheetRepository
        self.sheetState = AverySheetState()

        sheetRepository.statePublisher
            .receive(on: RunLoop.main)
            .sink { [weak self] state in
                self?.sheetState = state
            }
            .store(in: &cancellables)

        Task {
            await sheetRepository.load()
        }
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
