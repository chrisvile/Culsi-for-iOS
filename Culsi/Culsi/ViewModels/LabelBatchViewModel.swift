import Combine
import Foundation

@MainActor
final class LabelBatchViewModel: ObservableObject {
    @Published private(set) var sheetState: AverySheetState
    @Published var selectedLogs: Set<UUID> = []

    private let sheetRepository: AverySheetRepositoryType
    private var cancellables = Set<AnyCancellable>()

    init(sheetRepository: AverySheetRepositoryType = AverySheetRepository()) {
        self.sheetRepository = sheetRepository
        self.sheetState = AverySheetState(templateIdentifier: LabelTemplates.avery5160.id)

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
        let template = LabelTemplates.avery5160
        var placements: [AveryPlacement] = []
        for (index, log) in logs.enumerated() where index < template.capacity {
            let row = index / template.columns
            let column = index % template.columns
            let text = "\(log.name)\n\(Converters.displayDateFormatter.string(from: log.date))"
            placements.append(AveryPlacement(row: row, column: column, text: text))
        }
        sheetState.placements = placements
        Task {
            await sheetRepository.updatePlacements(placements)
        }
    }

    func reset() {
        Task { await sheetRepository.reset() }
    }
}
