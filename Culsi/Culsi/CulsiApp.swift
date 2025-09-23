//
//  CulsiApp.swift
//  Culsi
//
//  Created by Chris Vile on 9/23/25.
//

import SwiftUI
import CoreData

@main
struct CulsiApp: App {
    let persistenceController = PersistenceController.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, persistenceController.container.viewContext)
        }
    }
}
