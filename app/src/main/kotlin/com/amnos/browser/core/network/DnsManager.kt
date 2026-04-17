package com.amnos.browser.core.network

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.CookieJar
import com.amnos.browser.core.session.AmnosLog
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.Inet4Address
import java.net.InetAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object DnsManager {
    private val DOH_POOL = listOf(
        "https://1.1.1.1/dns-query",      // Cloudflare
        "https://dns.google/dns-query",   // Google
        "https://9.9.9.9/dns-query",      // Quad9
        "https://doh.mullvad.net/dns-query", // Mullvad
        "https://doh.mullvad.net/dns-query"  // Mullvad (Retry/Weight)
    )

    private val BOOTSTRAP_POOL = mapOf(
        "1.1.1.1" to listOf("1.1.1.1", "1.0.0.1"),
        "dns.google" to listOf("8.8.8.8", "8.8.4.4"),
        "9.9.9.9" to listOf("9.9.9.9", "149.112.112.112"),
        "doh.mullvad.net" to listOf("194.242.2.2")
    )

    @Volatile
    private var bootstrapClient = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .cookieJar(CookieJar.NO_COOKIES)
        .build()

    @Volatile
    private var dnsOverHttps: Dns = createDnsOverHttps(bootstrapClient)

    private fun createDnsOverHttps(client: OkHttpClient): Dns {
        val configUrl = com.amnos.browser.BuildConfig.SECURITY_DOH_URL
        val isDynamic = configUrl.uppercase() == "DYNAMIC"
        
        val urlToUse = if (isDynamic) {
            DOH_POOL[Random.nextInt(DOH_POOL.size)]
        } else {
            configUrl
        }

        AmnosLog.i("DnsManager", "Initializing DnsOverHttps (URL: $urlToUse, mode: ${if (isDynamic) "DYNAMIC" else "STATIC"})")
        
        val builder = DnsOverHttps.Builder()
            .client(client)
            .url(urlToUse.toHttpUrl())

        // Add bootstrap hosts if known to avoid circularity
        try {
            val host = urlToUse.toHttpUrl().host
            BOOTSTRAP_POOL[host]?.let { ips ->
                builder.bootstrapDnsHosts(ips.map { InetAddress.getByName(it) })
            }
        } catch (ignored: Exception) {}

        return builder.build()
    }

    fun lookup(hostname: String, blockIpv6: Boolean): List<InetAddress> {
        return try {
            AmnosLog.v("DnsManager", "Lookup DoH: $hostname")
            val resolved = dnsOverHttps.lookup(hostname)
            val addressList = resolved.joinToString { it.hostAddress ?: "unknown" }
            AmnosLog.v("DnsManager", "Resolved $hostname -> $addressList")
            
            if (!blockIpv6) {
                resolved
            } else {
                resolved.filterIsInstance<Inet4Address>().ifEmpty {
                    AmnosLog.w("DnsManager", "No IPv4 available for $hostname (IPv6 filtered)")
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
    private var ipv4OnlyClient: OkHttpClient? = null
    @Volatile
    private var dualStackClient: OkHttpClient? = null

    fun secureClient(blockIpv6: Boolean): OkHttpClient {
        val current = if (blockIpv6) ipv4OnlyClient else dualStackClient
        if (current != null) return current

        return synchronized(this) {
            val cached = if (blockIpv6) ipv4OnlyClient else dualStackClient
            cached ?: OkHttpClient.Builder()
                .proxy(Proxy.NO_PROXY)
                .dns(dns(blockIpv6))
                .cookieJar(CookieJar.NO_COOKIES)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build().also {
                    if (blockIpv6) {
                        ipv4OnlyClient = it
                    } else {
                        dualStackClient = it
                    }
                }
        }
    }

    fun destroyAndRebuild() {
        synchronized(this) {
            AmnosLog.w("DnsManager", "Network Rotation: Rebuilding HTTP clients and DNS state")
            
            ipv4OnlyClient?.dispatcher?.cancelAll()
            ipv4OnlyClient?.connectionPool?.evictAll()
            ipv4OnlyClient = null
            
            dualStackClient?.dispatcher?.cancelAll()
            dualStackClient?.connectionPool?.evictAll()
            dualStackClient = null
            
            bootstrapClient.dispatcher.cancelAll()
            bootstrapClient.connectionPool.evictAll()
            
            bootstrapClient = OkHttpClient.Builder()
                .proxy(Proxy.NO_PROXY)
                .cookieJar(CookieJar.NO_COOKIES)
                .build()
            
            dnsOverHttps = createDnsOverHttps(bootstrapClient)
        }
    }
}
