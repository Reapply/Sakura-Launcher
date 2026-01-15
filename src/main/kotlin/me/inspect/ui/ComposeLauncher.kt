package me.inspect.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
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

/**
 * ViewModel responsible for authentication flow and launcher state.
 */
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

    val isLoggedIn: Boolean
        get() = profile != null

    /**
     * Starts the Microsoft → Xbox → Minecraft authentication flow.
     */
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
                    error = redirectResult.error
                    return@launch
                }

                val msaTokens = withContext(Dispatchers.IO) {
                    authenticator.finishAuthorization(
                        redirectResult.code!!,
                        redirectResult.state!!,
                        pending.state,
                        pending.codeVerifier
                    )
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

    /**
     * Opens the system browser to the given URL.
     */
    private fun openBrowser(url: String) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}

/**
 * Remembers a single instance of [LauncherViewModel].
 */
@Composable
fun rememberLauncherViewModel(): LauncherViewModel =
    remember { LauncherViewModel() }

/**
 * Loads an image from the application resources and caches it as [ImageBitmap].
 */
@Composable
fun rememberResourceImage(path: String): ImageBitmap? =
    remember(path) {
        runCatching {
            ImageIO.read(
                object {}.javaClass.getResourceAsStream(path)
            )?.toComposeImageBitmap()
        }.getOrNull()
    }

/**
 * Draws a full-size background image with a bottom-to-top fade overlay.
 */
@Composable
fun BackgroundWithFade(
    modifier: Modifier = Modifier,
    imagePath: String,
    fadeColor: Color
) {
    val image = rememberResourceImage(imagePath)

    Box(modifier) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            fadeColor,
                            fadeColor.copy(alpha = 0.85f),
                            Color.Transparent
                        ),
                        startY = Float.POSITIVE_INFINITY,
                        endY = 0f
                    )
                )
        )
    }
}

/**
 * Root launcher UI rendered inside the window.
 */
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
            Box(Modifier.fillMaxSize()) {
                BackgroundWithFade(
                    modifier = Modifier.fillMaxSize(),
                    imagePath = "/images/background.png",
                    fadeColor = Colors.Background
                )

                Column(Modifier.fillMaxSize()) {
                    TitleBar(onClose, onMinimize)

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
}

@Composable
private fun WindowControlButton(
    baseColor: Color,
    hoverColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val color by animateColorAsState(
        targetValue = if (hovered) hoverColor else baseColor,
        animationSpec = tween(120),
        label = "window-button-hover"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(32.dp)
            .hoverable(interactionSource)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
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
                .height(40.dp)
                .background(Colors.Background)
                .padding(horizontal = 16.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp)),
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
                WindowControlButton(
                    baseColor = Color(0xFFFFBD2E),
                    hoverColor = Color(0xFFE0A800), // darker yellow
                    onClick = onMinimize
                )

                WindowControlButton(
                    baseColor = Color(0xFFFF5F57),
                    hoverColor = Color(0xFFE04842), // darker red
                    onClick = onClose
                )
            }
        }
    }
}

/**
 * Login screen shown when the user is not authenticated.
 */
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
            modifier = Modifier
                .width(200.dp)
                .height(48.dp)
        ) {
            Text(
                if (viewModel.isLoggingIn) "Logging in..." else "LOGIN",
                fontFamily = GoogleSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        viewModel.error?.let {
            Text(
                it,
                color = Colors.Accent,
                fontSize = 14.sp,
                fontFamily = GoogleSans
            )
        }
    }
}

/**
 * Profile view shown after successful login.
 */
@Composable
fun LoggedInView(profile: MinecraftProfile) {
    val playerHead by produceState<ImageBitmap?>(null, profile.id) {
        value = withContext(Dispatchers.IO) {
            try {
                val uuid = profile.id.replace("-", "")
                val url = URL("https://mc-heads.net/avatar/$uuid/128")
                ImageIO.read(url)?.toComposeImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        playerHead?.let {
            Surface(
                modifier = Modifier.size(128.dp),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 24.dp,
                color = Colors.SurfaceDark
            ) {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Fit
                )
            }
        } ?: CircularProgressIndicator(color = Colors.Accent)

        Text(
            profile.name,
            color = Colors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = GoogleSans
        )
    }
}
