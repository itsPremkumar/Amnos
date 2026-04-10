package com.privacy.browser.core.network

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException

object DnsManager {
    // Inspired by professional privacy tools
    // Uses Cloudflare (1.1.1.1) for secure, encrypted DNS lookups
    
    private val bootstrapClient = OkHttpClient.Builder().build()
    
    val dnsOverHttps: Dns by lazy {
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                listOf(
                    InetAddress.getByName("1.1.1.1"),
                    InetAddress.getByName("1.0.0.1"),
                    InetAddress.getByName("2606:4700:4700::1111"),
                    InetAddress.getByName("2606:4700:4700::1001")
                )
            )
            .build()
    }

    val secureClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(dnsOverHttps)
            .build()
    }
}
