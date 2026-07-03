// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "DevAuthHarness",
    platforms: [.iOS(.v16), .macOS(.v14)],
    products: [
        .executable(name: "DevAuthHarness", targets: ["DevAuthHarness"])
    ],
    dependencies: [
        .package(path: "../../Packages/DefdoAuthMobile")
    ],
    targets: [
        .executableTarget(
            name: "DevAuthHarness",
            dependencies: ["DefdoAuthMobile"],
            path: "Sources/DevAuthHarness"
        )
    ]
)
