package com.privacy.browser.core.network

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.CookieJar
import com.privacy.browser.core.session.AmnosLog
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.Inet4Address
import java.net.InetAddress
import java.net.Proxy

object DnsManager {
    private val bootstrapClient = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .cookieJar(CookieJar.NO_COOKIES)
        .build()

    private val dnsOverHttps: Dns by lazy {
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                listOf(
                    InetAddress.getByName("1.1.1.1"),
                    InetAddress.getByName("1.0.0.1")
                )
            )
            .build()
    }

    fun lookup(hostname: String, blockIpv6: Boolean): List<InetAddress> {
        return try {
            AmnosLog.d("DnsManager", "Resolving hostname via DoH: $hostname")
            val resolved = dnsOverHttps.lookup(hostname)
            AmnosLog.d("DnsManager", "Resolved $hostname to ${resolved.size} addresses")
            
            if (!blockIpv6) {
                resolved
            } else {
                resolved.filterIsInstance<Inet4Address>().ifEmpty { 
                    AmnosLog.d("DnsManager", "No IPv4 found for $hostname, falling back to all")
                    resolved 
                }
            }
        } catch (e: Exception) {
            AmnosLog.e("DnsManager", "DNS resolution FAILED for $hostname", e)
            throw e
        }
    }

    fun dns(blockIpv6: Boolean): Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return DnsManager.lookup(hostname, blockIpv6)
        }
    }

    @Volatile
    private var cachedClient: OkHttpClient? = null

    fun secureClient(blockIpv6: Boolean): OkHttpClient {
        val current = cachedClient
        if (current != null) return current

        return synchronized(this) {
            cachedClient ?: OkHttpClient.Builder()
                .proxy(Proxy.NO_PROXY)
                .dns(dns(blockIpv6))
                .cookieJar(CookieJar.NO_COOKIES)
                .build().also { cachedClient = it }
        }
    }
}
