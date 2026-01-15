package me.inspect.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.inspect.auth.Authenticator
import me.inspect.auth.LocalRedirectServer
import me.inspect.auth.MinecraftProfile
import java.awt.Desktop
import java.net.URI
import java.net.URL
import javax.imageio.ImageIO

class LauncherViewModel(
    private val authenticator: Authenticator = Authenticator(),
    private val redirectServerProvider: () -> LocalRedirectServer = { LocalRedirectServer() }
) {
    var isLoggingIn by mutableStateOf(false)
        private set
    var profile by mutableStateOf<MinecraftProfile?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val isLoggedIn: Boolean get() = profile != null

    fun login() {
        if (isLoggingIn) return
        isLoggingIn = true
        error = null

        scope.launch {
            try {
                val pending = withContext(Dispatchers.IO) {
                    authenticator.createAuthorization()
                }
                openBrowser(pending.authorizationUrl)

                val redirectResult = withContext(Dispatchers.IO) {
                    redirectServerProvider().waitForRedirect(pending.state)
                }

                if (!redirectResult.error.isNullOrBlank()) {
                    throw IllegalStateException("Authorization error: ${redirectResult.error}")
                }

                val code = redirectResult.code ?: error("Missing authorization code")
                val state = redirectResult.state ?: error("Missing state")

                val msaTokens = withContext(Dispatchers.IO) {
                    authenticator.finishAuthorization(code, state, pending.state, pending.codeVerifier)
                }

                val xblToken = withContext(Dispatchers.IO) {
                    authenticator.authenticateXbox(msaTokens.accessToken.token)
                }

                val xstsToken = withContext(Dispatchers.IO) {
                    authenticator.obtainXsts(xblToken.token)
                }

                val mcAccess = withContext(Dispatchers.IO) {
                    authenticator.authenticateMinecraft(xstsToken)
                }

                profile = withContext(Dispatchers.IO) {
                    authenticator.getMinecraftProfile(mcAccess.token)
                }
            } catch (t: Throwable) {
                error = t.localizedMessage ?: "Login failed"
            } finally {
                isLoggingIn = false
            }
        }
    }

    private fun openBrowser(url: String) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}

@Composable
fun rememberLauncherViewModel() = remember { LauncherViewModel() }

@Composable
fun WindowScope.LauncherApp(
    viewModel: LauncherViewModel = rememberLauncherViewModel(),
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {}
) {
    MaterialTheme(typography = SakuraTypography) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            color = Colors.Background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TitleBar(onClose = onClose, onMinimize = onMinimize)

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.isLoggedIn) {
                        LoggedInView(viewModel.profile!!)
                    } else {
                        LoginView(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun WindowScope.TitleBar(
    onClose: () -> Unit,
    onMinimize: () -> Unit
) {
    WindowDraggableArea {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Sakura Launcher",
                color = Colors.TextSecondary,
                fontSize = 14.sp,
                fontFamily = GoogleSans,
                fontWeight = FontWeight.Medium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFBD2E))
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5F57))
                    )
                }
            }
        }
    }
}

@Composable
fun LoginView(viewModel: LauncherViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Sakura Launcher",
            color = Colors.TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = GoogleSans
        )

        Button(
            onClick = { viewModel.login() },
            enabled = !viewModel.isLoggingIn,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Colors.Accent,
                contentColor = Colors.TextPrimary
            ),
            modifier = Modifier.height(48.dp).width(200.dp)
        ) {
            Text(
                if (viewModel.isLoggingIn) "Logging in..." else "LOGIN",
                fontFamily = GoogleSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        viewModel.error?.let { errorMessage ->
            Text(
                errorMessage,
                color = Colors.Accent,
                fontSize = 14.sp,
                fontFamily = GoogleSans
            )
        }
    }
}

@Composable
fun LoggedInView(profile: MinecraftProfile) {
    val playerHead by produceState<ImageBitmap?>(null, profile.id) {
        value = withContext(Dispatchers.IO) {
            try {
                val uuid = profile.id.replace("-", "")
                val url = URL("https://mc-heads.net/avatar/$uuid/128")
                val connection = url.openConnection().apply {
                    setRequestProperty("User-Agent", "SakuraLauncher/1.0")
                }
                val image = ImageIO.read(connection.getInputStream())
                image?.toComposeImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        playerHead?.let { bitmap ->
            Surface(
                modifier = Modifier.size(128.dp),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 24.dp,
                color = Colors.SurfaceDark
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Player Head",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None
                )
            }
        } ?: Box(
            modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Colors.Accent)
        }

        Text(
            profile.name,
            color = Colors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = GoogleSans
        )
    }
}
