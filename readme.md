# Culsi iOS

Culsi iOS is the iOS version of the **Culsi app**, a nutrition and labeling tool designed to help track food logs, manage item catalogs, and print Avery-style labels.  
This project is a **SwiftUI + Combine** port of the existing Culsi Android codebase (originally written in Kotlin with Room & ViewModels).

## 🚀 Features
- Track and manage **food logs**
- Maintain an **item catalog**
- Generate and preview **Avery sheet labels**
- Print labels via **LabelPrinter integration**
- Export logs in multiple **formats** (CSV, JSON, etc.)
- TPHC (4-hour) timers with live countdown and expiration
- Hot/Cold hold logging with temperature capture
- Labels include discard time (TPHC) or latest temperature

## 📸 Screenshots
> _Add screenshots here once the UI is ready_

| Food Log | Label Preview | Item Catalog |
|----------|---------------|--------------|
| ![FoodLog](docs/images/foodlog.png) | ![LabelPreview](docs/images/labels.png) | ![Catalog](docs/images/catalog.png) |

## 🛠 Installation
1. Clone the repo:
   ```sh
   git clone https://github.com/YOUR_USERNAME/culsi-ios.git
   cd culsi-ios
   ```
2. Open in Xcode:
   ```sh
   open Culsi.xcodeproj
   ```
3. Install dependencies (if using Swift Package Manager):
   ```sh
   xcodebuild -resolvePackageDependencies
   ```
4. Run on simulator or a real device.

## 📂 Project Structure
This project follows a **Kotlin → Swift migration plan**.  
Each Android component has a Swift counterpart:

| Android (Kotlin)             | iOS (Swift)                | Notes |
|-------------------------------|----------------------------|-------|
| `CulsiDb.kt` (Room database) | `CulsiDatabase.swift`      | Core Data / SQLite wrapper |
| `Converters.kt`              | `Converters.swift`         | Codable / custom transforms |
| `FoodLog.kt`                 | `FoodLog.swift`            | Entity model |
| `FoodLogDao.kt`              | `FoodLogDao.swift`         | Repository/Data layer |
| `FoodLogRepository.kt`       | `FoodLogRepository.swift`  | Uses Combine publishers |
| `ItemCatalogRepository.kt`   | `ItemCatalogRepository.swift` | — |
| `AverySheetState.kt`         | `AverySheetState.swift`    | Model for UI state |
| `AverySheetStateDao.kt`      | `AverySheetDao.swift`      | — |
| `AverySheetRepository.kt`    | `AverySheetRepository.swift` | — |
| `AveryPlacement.kt`          | `AveryPlacement.swift`     | — |
| `FoodLogViewModel.kt`        | `FoodLogViewModel.swift`   | ObservableObject |
| `LabelBatchViewModel.kt`     | `LabelBatchViewModel.swift` | — |
| `ItemCatalogViewModel.kt`    | `ItemCatalogViewModel.swift` | — |
| `LabelPrinter.kt`            | `LabelPrinter.swift`       | iOS printing APIs |
| `LabelTemplates.kt`          | `LabelTemplates.swift`     | Template models |
| `ExportFormat.kt`             | `ExportFormat.swift`       | Enum in Swift |
| `DiscardAction.kt`            | `DiscardAction.swift`      | Enum in Swift |
| `MainActivity.kt`             | `CulsiApp.swift`           | SwiftUI entry point |

## 🧑‍💻 Contributing
Pull requests are welcome!  
For major changes, please open an issue first to discuss what you’d like to change.  

⚠️ Tests will be added in a later phase. Currently, no unit tests are included.  

## 📜 License
This project is licensed under the **MIT License** – see the [LICENSE](LICENSE) file for details.

