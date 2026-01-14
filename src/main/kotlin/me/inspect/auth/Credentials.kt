package me.inspect.auth

import java.time.Instant

enum class CredentialStage {
    MinecraftAccess,
    Xsts,
    XboxLive,
    MsaAccess,
    MsaRefresh,
    None
}

data class AccountCredentials(
    val msaAccess: TokenWithExpiry? = null,
    val msaRefresh: String? = null,
    val xblToken: TokenWithExpiry? = null,
    val xstsToken: XstsToken? = null,
    val minecraftAccess: TokenWithExpiry? = null,
    val minecraftProfile: MinecraftProfile? = null
) {
    fun stage(now: Instant = Instant.now()): CredentialStage {
        return when {
            minecraftAccess?.isValid(now) == true -> CredentialStage.MinecraftAccess
            xstsToken?.isValid(now) == true -> CredentialStage.Xsts
            xblToken?.isValid(now) == true -> CredentialStage.XboxLive
            msaAccess?.isValid(now) == true -> CredentialStage.MsaAccess
            msaRefresh != null -> CredentialStage.MsaRefresh
            else -> CredentialStage.None
        }
    }
}
