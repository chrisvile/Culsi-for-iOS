import Combine
import Foundation

protocol ItemCatalogRepositoryType {
    var itemsPublisher: AnyPublisher<[CatalogItem], Never> { get }
    func refresh() async
    func addOrUpdate(_ input: CatalogItemInput) async throws
    func delete(id: UUID) async throws
}

final class ItemCatalogRepository: ItemCatalogRepositoryType {
    private let store: ItemCatalogStore
    private let subject = CurrentValueSubject<[CatalogItem], Never>([])

    var itemsPublisher: AnyPublisher<[CatalogItem], Never> {
        subject.eraseToAnyPublisher()
    }

    init(store: ItemCatalogStore = ItemCatalogStore()) {
        self.store = store
        Task { await refresh() }
    }

    func refresh() async {
        do {
            let items = try await store.fetchAll()
            subject.send(items)
        } catch {
            #if DEBUG
            print("Failed to fetch catalog items: \(error)")
            #endif
            subject.send([])
        }
    }

    func addOrUpdate(_ input: CatalogItemInput) async throws {
        _ = try await store.upsert(from: input)
        await refresh()
    }

    func delete(id: UUID) async throws {
        try await store.delete(id: id)
        await refresh()
    }
}
