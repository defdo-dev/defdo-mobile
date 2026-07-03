#if canImport(SwiftUI)
import SwiftUI
import DefdoThemeMobile

/// Maps a Defdo semantic token to a SwiftUI Color, with a safe default.
extension ThemeTokens {
    func color(_ key: String, default fallback: Color) -> Color {
        guard let hex = tokens[key], let parsed = Color(hexString: hex) else { return fallback }
        return parsed
    }
}

extension Color {
    init?(hexString: String) {
        guard hexString.hasPrefix("#"), hexString.count == 7 else { return nil }
        let hex = String(hexString.dropFirst())
        guard let value = UInt32(hex, radix: 16) else { return nil }
        self.init(
            red: Double((value >> 16) & 0xFF) / 255.0,
            green: Double((value >> 8) & 0xFF) / 255.0,
            blue: Double(value & 0xFF) / 255.0
        )
    }
}

private struct Scaffold<Content: View>: View {
    let theme: ThemeTokens
    @ViewBuilder let content: () -> Content

    var body: some View {
        ZStack {
            theme.color("color.background.primary", default: .white).ignoresSafeArea()
            VStack(spacing: 16) { content() }.padding(24)
        }
    }
}

private func title(_ theme: ThemeTokens, _ text: String) -> some View {
    Text(text)
        .font(.title2)
        .multilineTextAlignment(.center)
        .foregroundColor(theme.color("color.text.primary", default: .black))
}

private func subtitle(_ theme: ThemeTokens, _ text: String) -> some View {
    Text(text)
        .font(.body)
        .multilineTextAlignment(.center)
        .foregroundColor(theme.color("color.text.muted", default: .gray))
}

public struct LaunchView: View {
    let theme: ThemeTokens
    public init(theme: ThemeTokens) { self.theme = theme }
    public var body: some View {
        Scaffold(theme: theme) {
            ProgressView()
            title(theme, "Defdo SelfCare")
        }
    }
}

public struct SignedOutView: View {
    let theme: ThemeTokens
    let onLogin: () -> Void
    public init(theme: ThemeTokens, onLogin: @escaping () -> Void) {
        self.theme = theme; self.onLogin = onLogin
    }
    public var body: some View {
        Scaffold(theme: theme) {
            title(theme, "Welcome")
            subtitle(theme, "Sign in to manage your account.")
            Button("Log in", action: onLogin)
        }
    }
}

public struct BootstrapLoadingView: View {
    let theme: ThemeTokens
    public init(theme: ThemeTokens) { self.theme = theme }
    public var body: some View {
        Scaffold(theme: theme) {
            ProgressView()
            subtitle(theme, "Setting things up…")
        }
    }
}

public struct NeedsLineLinkingView: View {
    let theme: ThemeTokens
    public init(theme: ThemeTokens) { self.theme = theme }
    public var body: some View {
        Scaffold(theme: theme) {
            title(theme, "Almost there")
            subtitle(theme, "We need to link a line to your account. (Coming soon.)")
        }
    }
}

public struct ReadyHomeView: View {
    let theme: ThemeTokens
    let tenantLabel: String?
    public init(theme: ThemeTokens, tenantLabel: String?) {
        self.theme = theme; self.tenantLabel = tenantLabel
    }
    public var body: some View {
        Scaffold(theme: theme) {
            title(theme, "You're all set")
            subtitle(theme, tenantLabel.map { "Connected to \($0)." } ?? "Welcome back.")
        }
    }
}

public struct ErrorView: View {
    let theme: ThemeTokens
    let message: String
    let onRetry: (() -> Void)?
    public init(theme: ThemeTokens, message: String, onRetry: (() -> Void)?) {
        self.theme = theme; self.message = message; self.onRetry = onRetry
    }
    public var body: some View {
        Scaffold(theme: theme) {
            title(theme, "Something went wrong")
            subtitle(theme, message)
            if let onRetry { Button("Retry", action: onRetry) }
        }
    }
}
#endif
