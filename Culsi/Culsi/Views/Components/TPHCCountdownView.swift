import Combine
import SwiftUI

struct TPHCCountdownView: View {
    let startedAt: Date

    @State private var now = Date()
    private let timer = Timer.publish(every: 30, on: .main, in: .common).autoconnect()

    private static let countdownFormatter: DateComponentsFormatter = {
        let formatter = DateComponentsFormatter()
        formatter.unitsStyle = .abbreviated
        formatter.allowedUnits = [.hour, .minute, .second]
        return formatter
    }()

    var body: some View {
        let discardAt = startedAt.addingTimeInterval(4 * 60 * 60)
        let remaining = max(0, discardAt.timeIntervalSince(now))

        let message: String
        if remaining > 0 {
            let value = Self.countdownFormatter.string(from: remaining) ?? "--"
            message = "Discard in \(value)"
        } else {
            message = "Expired"
        }

        return Text(message)
            .font(.caption)
            .foregroundStyle(remaining > 0 ? Color.blue : .red)
            .onReceive(timer) { value in
                now = value
            }
    }
}
