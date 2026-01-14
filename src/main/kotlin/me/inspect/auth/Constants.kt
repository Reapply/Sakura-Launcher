package me.inspect.auth

object AuthConstants {
    // waiting on mojang to accept ts (annoying)
    const val CLIENT_ID = ""
    const val REDIRECT_URI = "http://localhost:3160/auth"
    const val MSA_SCOPES = "XboxLive.signin XboxLive.offline_access"

    const val MSA_AUTHORIZE_URL = "https://login.live.com/oauth20_authorize.srf"
    const val MSA_TOKEN_URL = "https://login.live.com/oauth20_token.srf"

    const val XBL_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate"
    const val XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"

    const val MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox"
    const val MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"
}
