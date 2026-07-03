// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "DefdoSelfCare",
    platforms: [.iOS(.v16), .macOS(.v14)],
    products: [
        .library(name: "DefdoSelfCareKit", targets: ["DefdoSelfCareKit"]),
        .executable(name: "DefdoSelfCare", targets: ["DefdoSelfCare"])
    ],
    dependencies: [
        .package(path: "../../Packages/DefdoAuthMobile"),
        .package(path: "../../Packages/DefdoThemeMobile")
    ],
    targets: [
        // App-shell logic + SwiftUI screens. Lives in a library so it is
        // testable with `swift test` on macOS without an iOS simulator.
        .target(
            name: "DefdoSelfCareKit",
            dependencies: ["DefdoAuthMobile", "DefdoThemeMobile"],
            path: "Sources/DefdoSelfCareKit"
        ),
        // Thin app entry point.
        .executableTarget(
            name: "DefdoSelfCare",
            dependencies: ["DefdoSelfCareKit", "DefdoAuthMobile", "DefdoThemeMobile"],
            path: "Sources/DefdoSelfCare"
        ),
        .testTarget(
            name: "DefdoSelfCareTests",
            dependencies: ["DefdoSelfCareKit"],
            path: "Tests/DefdoSelfCareTests"
        )
    ]
)
