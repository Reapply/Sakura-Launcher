package me.inspect

import me.inspect.auth.Authenticator
import me.inspect.auth.LocalRedirectServer

fun main() {
    val authenticator = Authenticator()
    val pending = authenticator.createAuthorization()
    println("Open this URL in a browser to sign in:")
    println(pending.authorizationUrl)

    val redirectServer = LocalRedirectServer()
    val result = redirectServer.waitForRedirect(pending.state)
    if (!result.error.isNullOrBlank()) {
        error("Authorization error: ${result.error} ${result.errorDescription.orEmpty()}")
    }
    val code = result.code ?: error("No authorization code returned")
    val state = result.state ?: error("No state returned")

    val msaTokens = authenticator.finishAuthorization(code, state, pending.state, pending.codeVerifier)
    val xblToken = authenticator.authenticateXbox(msaTokens.accessToken.token)
    val xstsToken = authenticator.obtainXsts(xblToken.token)
    val mcAccess = authenticator.authenticateMinecraft(xstsToken)
    val profile = authenticator.getMinecraftProfile(mcAccess.token)

    println("Minecraft profile: ${profile.name} (${profile.id})")
}
