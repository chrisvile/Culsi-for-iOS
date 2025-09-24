import Combine
import Foundation

final class TPHCService {
    private var cancellable: AnyCancellable?
    private let repository: FoodLogRepositoryType
    private let interval: TimeInterval

    init(repository: FoodLogRepositoryType, interval: TimeInterval = 30) {
        self.repository = repository
        self.interval = interval
    }

    func start() {
        guard cancellable == nil else { return }
        cancellable = Timer.publish(every: interval, on: .main, in: .common)
            .autoconnect()
            .prepend(Date())
            .sink { [weak self] _ in
                guard let self else { return }
                Task { await self.repository.refresh() }
            }
    }

    func stop() {
        cancellable?.cancel()
        cancellable = nil
    }
}
