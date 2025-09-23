import Combine
import Foundation

@MainActor
final class ItemCatalogViewModel: ObservableObject {
    @Published private(set) var items: [CatalogItem] = []
    @Published var searchText: String = ""

    private let repository: ItemCatalogRepositoryType
    private var cancellables = Set<AnyCancellable>()
    private var allItems: [CatalogItem] = []

    init(repository: ItemCatalogRepositoryType = ItemCatalogRepository()) {
        self.repository = repository

        repository.itemsPublisher
            .receive(on: RunLoop.main)
            .sink { [weak self] items in
                self?.allItems = items
                self?.applyFilters()
            }
            .store(in: &cancellables)

        $searchText
            .removeDuplicates()
            .debounce(for: .milliseconds(250), scheduler: RunLoop.main)
            .sink { [weak self] _ in
                self?.applyFilters()
            }
            .store(in: &cancellables)

        Task { await repository.refresh() }
    }

    func addOrUpdate(_ input: CatalogItemInput) {
        Task { try? await repository.addOrUpdate(input) }
    }

    func delete(id: UUID) {
        Task { try? await repository.delete(id: id) }
    }

    private func applyFilters() {
        guard !searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            items = allItems
            return
        }
        items = allItems.filter { item in
            item.name.localizedCaseInsensitiveContains(searchText) || (item.notes ?? "").localizedCaseInsensitiveContains(searchText)
        }
    }
}
