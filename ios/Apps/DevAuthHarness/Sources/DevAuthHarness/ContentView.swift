import SwiftUI

struct DevAuthHarnessView: View {
    @StateObject private var viewModel = DevAuthViewModel()

    var body: some View {
        VStack(spacing: 16) {
            Text(viewModel.statusText)
                .font(.body)
                .multilineTextAlignment(.center)
                .padding()

            Button("Start Login") {
                Task { await viewModel.startLogin() }
            }
            .disabled(!viewModel.isConfigured)
            .buttonStyle(.borderedProminent)

            Button("Refresh") {
                Task { await viewModel.refresh() }
            }
            .disabled(!viewModel.isConfigured)
            .buttonStyle(.bordered)

            Button("Logout") {
                Task { await viewModel.logout() }
            }
            .disabled(!viewModel.isConfigured)
            .buttonStyle(.bordered)

            Button("Clear Local Session") {
                Task { await viewModel.clear() }
            }
            .disabled(!viewModel.isConfigured)
            .buttonStyle(.bordered)
        }
        .padding()
    }
}
