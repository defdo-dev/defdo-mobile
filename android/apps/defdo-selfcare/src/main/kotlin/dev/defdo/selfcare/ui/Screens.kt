package dev.defdo.selfcare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.defdo.mobile.theme.ThemeTokens

/** Maps a Defdo semantic token to a Compose Color, with a safe default. */
private fun ThemeTokens.color(key: String, default: Color): Color {
    val hex = tokens[key] ?: return default
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(default)
}

@Composable
private fun ScreenScaffold(
    theme: ThemeTokens,
    content: @Composable ColumnScopeLike.() -> Unit
) {
    val bg = theme.color("color.background.primary", Color.White)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ColumnScopeLike(theme).content()
        }
    }
}

/** Small helper carrying theme into the column content lambda. */
class ColumnScopeLike(val theme: ThemeTokens)

@Composable
private fun Title(theme: ThemeTokens, text: String) {
    Text(
        text = text,
        color = theme.color("color.text.primary", Color.Black),
        fontSize = 22.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun Subtitle(theme: ThemeTokens, text: String) {
    Text(
        text = text,
        color = theme.color("color.text.muted", Color.Gray),
        fontSize = 15.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PrimaryButton(theme: ThemeTokens, label: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(label, color = theme.color("color.action.primary.text", Color.White))
    }
}

@Composable
fun LaunchScreen(theme: ThemeTokens) {
    ScreenScaffold(theme) {
        CircularProgressIndicator()
        Title(this.theme, "Defdo SelfCare")
    }
}

@Composable
fun SignedOutScreen(theme: ThemeTokens, onLogin: () -> Unit) {
    ScreenScaffold(theme) {
        Title(this.theme, "Welcome")
        Subtitle(this.theme, "Sign in to manage your account.")
        PrimaryButton(this.theme, "Log in", onLogin)
    }
}

@Composable
fun BootstrapLoadingScreen(theme: ThemeTokens) {
    ScreenScaffold(theme) {
        CircularProgressIndicator()
        Subtitle(this.theme, "Setting things up…")
    }
}

@Composable
fun NeedsLineLinkingScreen(theme: ThemeTokens) {
    ScreenScaffold(theme) {
        Title(this.theme, "Almost there")
        Subtitle(this.theme, "We need to link a line to your account. (Coming soon.)")
    }
}

@Composable
fun ReadyHomeScreen(theme: ThemeTokens, tenantLabel: String?) {
    ScreenScaffold(theme) {
        Title(this.theme, "You're all set")
        Subtitle(this.theme, tenantLabel?.let { "Connected to $it." } ?: "Welcome back.")
    }
}

@Composable
fun ErrorScreen(theme: ThemeTokens, message: String, onRetry: (() -> Unit)?) {
    ScreenScaffold(theme) {
        Title(this.theme, "Something went wrong")
        Subtitle(this.theme, message)
        if (onRetry != null) {
            PrimaryButton(this.theme, "Retry", onRetry)
        }
    }
}
