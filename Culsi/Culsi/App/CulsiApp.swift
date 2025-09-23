import Combine
import SwiftData
import SwiftUI

@main
struct CulsiApp: App {
    private let database = CulsiDatabase.shared

    init() {
        database.seedIfNeeded()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .modelContainer(database.container)
        }
    }
}

struct RootView: View {
    var body: some View {
        TabView {
            FoodLogListView()
                .tabItem { Label("Food Log", systemImage: "list.bullet") }
            ItemCatalogView()
                .tabItem { Label("Catalog", systemImage: "shippingbox") }
            LabelPreviewView()
                .tabItem { Label("Labels", systemImage: "printer") }
        }
    }
}
