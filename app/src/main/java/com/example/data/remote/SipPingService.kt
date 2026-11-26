package com.example.data.remote

import com.example.domain.model.SipAccount
import com.example.domain.model.SipTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SipPingService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    /**
     * Measures round-trip latency to a SIP server in milliseconds.
     * Uses HTTP/HTTPS via OkHttp for WSS/HTTPS endpoints or TCP socket handshake for SIP ports.
     */
    suspend fun pingServer(account: SipAccount): Long? = withContext(Dispatchers.IO) {
        val host = account.domain.trim()
        if (host.isBlank()) return@withContext null
        val port = if (account.port > 0) account.port else when (account.transport) {
            SipTransport.TLS -> 5061
            SipTransport.WSS -> 443
            else -> 5060
        }

        if (account.transport == SipTransport.WSS) {
            val url = "https://$host:$port/"
            val req = Request.Builder().url(url).head().build()
            val startNs = System.nanoTime()
            return@withContext try {
                okHttpClient.newCall(req).execute().use {
                    ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
                }
            } catch (_: Exception) {
                measureTcpHandshake(host, port)
            }
        }

        measureTcpHandshake(host, port)
    }

    private fun measureTcpHandshake(host: String, port: Int): Long? {
        val startNs = System.nanoTime()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 3_000)
            }
            ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
        } catch (_: Exception) {
            // Fallback HTTP probe via OkHttp if raw SIP port is UDP-only or firewalled
            try {
                val req = Request.Builder().url("https://$host/").head().build()
                val httpStart = System.nanoTime()
                okHttpClient.newCall(req).execute().use {
                    ((System.nanoTime() - httpStart) / 1_000_000L).coerceAtLeast(1L)
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
