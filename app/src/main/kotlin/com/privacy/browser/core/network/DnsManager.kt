package com.privacy.browser.core.network

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.CookieJar
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.Inet4Address
import java.net.InetAddress

object DnsManager {
    private val bootstrapClient = OkHttpClient.Builder()
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
        val resolved = dnsOverHttps.lookup(hostname)
        return if (!blockIpv6) {
            resolved
        } else {
            resolved.filterIsInstance<Inet4Address>().ifEmpty { resolved }
        }
    }

    fun dns(blockIpv6: Boolean): Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return DnsManager.lookup(hostname, blockIpv6)
        }
    }

    fun secureClient(blockIpv6: Boolean): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(dns(blockIpv6))
            .cookieJar(CookieJar.NO_COOKIES)
            .build()
    }
}
