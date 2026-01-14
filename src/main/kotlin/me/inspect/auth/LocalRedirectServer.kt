package me.inspect.auth

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class RedirectResult(
    val code: String?,
    val state: String?,
    val error: String?,
    val errorDescription: String?
)

class LocalRedirectServer(
    private val port: Int = 3160,
    private val path: String = "/auth"
) {
    fun waitForRedirect(expectedState: String, timeout: Duration = Duration.ofMinutes(5)): RedirectResult {
        val future = CompletableFuture<RedirectResult>()
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext(path) { exchange ->
            val result = handleExchange(exchange, expectedState)
            if (!future.isDone) {
                future.complete(result)
            }
            server.stop(0)
        }
        server.executor = Executors.newSingleThreadExecutor()
        server.start()
        return try {
            future.get(timeout.toMillis(), TimeUnit.MILLISECONDS)
        } finally {
            server.stop(0)
        }
    }

    private fun handleExchange(exchange: HttpExchange, expectedState: String): RedirectResult {
        val params = parseQuery(exchange.requestURI.rawQuery)
        val code = params["code"]
        val state = params["state"]
        val error = params["error"]
        val errorDescription = params["error_description"]

        val (status, body) = if (state != null && state != expectedState) {
            400 to AUTH_PAGE_FAILURE
        } else {
            200 to AUTH_PAGE_SUCCESS
        }

        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }

        return RedirectResult(code, state, error, errorDescription)
    }

    private fun parseQuery(query: String?): Map<String, String> {
        if (query.isNullOrBlank()) return emptyMap()
        return query.split("&").mapNotNull { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.isEmpty()) return@mapNotNull null
            val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8)
            val value = if (parts.size > 1) {
                URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
            } else {
                ""
            }
            key to value
        }.toMap()
    }

    companion object {
        private const val AUTH_PAGE_SUCCESS = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <title>Authorization Complete</title>
              <style>
                body { font-family: Arial, sans-serif; padding: 24px; }
                .card { max-width: 540px; margin: 0 auto; }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>Login complete</h1>
                <p>You may return to the launcher.</p>
              </div>
            </body>
            </html>
        """

        private const val AUTH_PAGE_FAILURE = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <title>Authorization Failed</title>
              <style>
                body { font-family: Arial, sans-serif; padding: 24px; }
                .card { max-width: 540px; margin: 0 auto; }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>Login failed</h1>
                <p>State did not match. Please return to the launcher and retry.</p>
              </div>
            </body>
            </html>
        """
    }
}
