package me.inspect.auth

interface SecretStorage {
    fun load(): AccountCredentials?
    fun save(credentials: AccountCredentials)
    fun clear()
}

class InMemorySecretStorage : SecretStorage {
    private var stored: AccountCredentials? = null

    override fun load(): AccountCredentials? = stored

    override fun save(credentials: AccountCredentials) {
        stored = credentials
    }

    override fun clear() {
        stored = null
    }
}
