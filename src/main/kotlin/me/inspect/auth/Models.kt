package me.inspect.auth

import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PendingAuthorization(
    val authorizationUrl: String,
    val state: String,
    val codeVerifier: String
)

data class TokenWithExpiry(
    val token: String,
    val expiry: Instant
) {
    fun isValid(now: Instant = Instant.now()): Boolean = now.isBefore(expiry)
}

data class XstsToken(
    val token: String,
    val expiry: Instant,
    val userHash: String
) {
    fun isValid(now: Instant = Instant.now()): Boolean = now.isBefore(expiry)
}

data class MsaTokens(
    val accessToken: TokenWithExpiry,
    val refreshToken: String?
)

@Serializable
data class MsaTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long
)

@Serializable
data class XboxLiveAuthRequest(
    @SerialName("RelyingParty") val relyingParty: String,
    @SerialName("TokenType") val tokenType: String,
    @SerialName("Properties") val properties: XboxLiveAuthProperties
)

@Serializable
data class XboxLiveAuthProperties(
    @SerialName("AuthMethod") val authMethod: String,
    @SerialName("SiteName") val siteName: String,
    @SerialName("RpsTicket") val rpsTicket: String
)

@Serializable
data class XboxLiveAuthResponse(
    @SerialName("IssueInstant") val issueInstant: String,
    @SerialName("NotAfter") val notAfter: String,
    @SerialName("Token") val token: String
)

@Serializable
data class XstsRequest(
    @SerialName("RelyingParty") val relyingParty: String,
    @SerialName("TokenType") val tokenType: String,
    @SerialName("Properties") val properties: XstsProperties
)

@Serializable
data class XstsProperties(
    @SerialName("SandboxId") val sandboxId: String,
    @SerialName("UserTokens") val userTokens: List<String>
)

@Serializable
data class XstsResponse(
    @SerialName("IssueInstant") val issueInstant: String,
    @SerialName("NotAfter") val notAfter: String,
    @SerialName("Token") val token: String,
    @SerialName("DisplayClaims") val displayClaims: XstsDisplayClaims? = null
)

@Serializable
data class XstsDisplayClaims(
    @SerialName("xui") val xui: List<XstsUserHash>? = null
)

@Serializable
data class XstsUserHash(
    @SerialName("uhs") val uhs: String
)

@Serializable
data class MinecraftLoginRequest(
    @SerialName("identityToken") val identityToken: String
)

@Serializable
data class MinecraftLoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("token_type") val tokenType: String? = null
)

@Serializable
data class MinecraftProfile(
    val id: String,
    val name: String,
    val skins: List<MinecraftSkin> = emptyList(),
    val capes: List<MinecraftCape> = emptyList()
)

@Serializable
data class MinecraftSkin(
    val id: String? = null,
    val state: String? = null,
    val url: String? = null,
    val variant: String? = null
)

@Serializable
data class MinecraftCape(
    val id: String? = null,
    val state: String? = null,
    val url: String? = null,
    val alias: String? = null
)

@Serializable
data class MinecraftError(
    val error: String? = null,
    val errorMessage: String? = null,
    val error_description: String? = null
)
