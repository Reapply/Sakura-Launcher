package me.inspect.auth

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class Authenticator(
    private val clientId: String = AuthConstants.CLIENT_ID,
    private val redirectUri: String = AuthConstants.REDIRECT_URI,
    private val client: HttpClient = HttpClient.newBuilder().build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun createAuthorization(): PendingAuthorization {
        val state = randomUrlSafeString(32)
        val verifier = randomUrlSafeString(64)
        val challenge = base64Url(sha256(verifier))
        val authUrl = buildUrl(
            AuthConstants.MSA_AUTHORIZE_URL,
            mapOf(
                "client_id" to clientId,
                "response_type" to "code",
                "redirect_uri" to redirectUri,
                "scope" to AuthConstants.MSA_SCOPES,
                "prompt" to "select_account",
                "state" to state,
                "code_challenge" to challenge,
                "code_challenge_method" to "S256"
            )
        )
        return PendingAuthorization(authUrl, state, verifier)
    }

    fun finishAuthorization(
        authorizationCode: String,
        stateFromRedirect: String,
        expectedState: String,
        codeVerifier: String
    ): MsaTokens {
        if (stateFromRedirect != expectedState) {
            throw IllegalStateException("CSRF state mismatch")
        }
        val response = postForm(
            AuthConstants.MSA_TOKEN_URL,
            mapOf(
                "client_id" to clientId,
                "grant_type" to "authorization_code",
                "code" to authorizationCode,
                "redirect_uri" to redirectUri,
                "code_verifier" to codeVerifier
            )
        )
        val token = json.decodeFromString<MsaTokenResponse>(response)
        val expiry = Instant.now().plusSeconds(token.expiresIn)
        return MsaTokens(TokenWithExpiry(token.accessToken, expiry), token.refreshToken)
    }

    fun refreshMsa(refreshToken: String): MsaTokens {
        val response = postForm(
            AuthConstants.MSA_TOKEN_URL,
            mapOf(
                "client_id" to clientId,
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "redirect_uri" to redirectUri,
                "scope" to AuthConstants.MSA_SCOPES
            )
        )
        val token = json.decodeFromString<MsaTokenResponse>(response)
        val expiry = Instant.now().plusSeconds(token.expiresIn)
        return MsaTokens(TokenWithExpiry(token.accessToken, expiry), token.refreshToken)
    }

    fun authenticateXbox(msaAccessToken: String): TokenWithExpiry {
        val request = XboxLiveAuthRequest(
            relyingParty = "http://auth.xboxlive.com",
            tokenType = "JWT",
            properties = XboxLiveAuthProperties(
                authMethod = "RPS",
                siteName = "user.auth.xboxlive.com",
                rpsTicket = "d=$msaAccessToken"
            )
        )
        val response = postJson(AuthConstants.XBL_AUTH_URL, request)
        val xbl = json.decodeFromString<XboxLiveAuthResponse>(response)
        val expiry = Instant.parse(xbl.notAfter)
        return TokenWithExpiry(xbl.token, expiry)
    }

    fun obtainXsts(xblToken: String): XstsToken {
        val request = XstsRequest(
            relyingParty = "rp://api.minecraftservices.com/",
            tokenType = "JWT",
            properties = XstsProperties(
                sandboxId = "RETAIL",
                userTokens = listOf(xblToken)
            )
        )
        val response = postJson(AuthConstants.XSTS_AUTH_URL, request)
        val xsts = json.decodeFromString<XstsResponse>(response)
        val userHash = xsts.displayClaims?.xui?.firstOrNull()?.uhs
            ?: throw IllegalStateException("Missing XSTS user hash")
        val expiry = Instant.parse(xsts.notAfter)
        return XstsToken(xsts.token, expiry, userHash)
    }

    fun authenticateMinecraft(xstsToken: XstsToken): TokenWithExpiry {
        val request = MinecraftLoginRequest("XBL3.0 x=${xstsToken.userHash};${xstsToken.token}")
        val response = postJson(AuthConstants.MC_LOGIN_URL, request)
        val mc = json.decodeFromString<MinecraftLoginResponse>(response)
        val expiry = Instant.now().plusSeconds(mc.expiresIn)
        return TokenWithExpiry(mc.accessToken, expiry)
    }

    fun getMinecraftProfile(minecraftAccessToken: String): MinecraftProfile {
        val request = HttpRequest.newBuilder(URI.create(AuthConstants.MC_PROFILE_URL))
            .header("Authorization", "Bearer $minecraftAccessToken")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("Minecraft profile request failed: ${response.statusCode()}")
        }
        return json.decodeFromString(response.body())
    }

    private inline fun <reified T> postJson(url: String, payload: T): String {
        val body = json.encodeToString(payload)
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return sendRequest(request)
    }

    private fun postForm(url: String, params: Map<String, String>): String {
        val body = params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return sendRequest(request)
    }

    private fun sendRequest(request: HttpRequest): String {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("HTTP ${response.statusCode()}: ${response.body()}")
        }
        return response.body()
    }

    private fun buildUrl(base: String, params: Map<String, String>): String {
        val query = params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return "$base?$query"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun randomUrlSafeString(length: Int): String {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return base64Url(bytes)
    }

    private fun sha256(value: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(StandardCharsets.US_ASCII))
    }

    private fun base64Url(bytes: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
