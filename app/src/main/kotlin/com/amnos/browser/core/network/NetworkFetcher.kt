package com.amnos.browser.core.network

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.amnos.browser.core.fingerprint.DeviceProfile
import com.amnos.browser.core.security.PrivacyPolicy
import com.amnos.browser.core.session.AmnosLog
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.ByteArrayInputStream

class NetworkFetcher(
    private val policyProvider: () -> PrivacyPolicy
) {
    fun fetchResponse(
        request: WebResourceRequest,
        decision: RequestDecision,
        profile: DeviceProfile,
        topLevelHost: String?
    ): WebResourceResponse? {
        val policy = policyProvider()
        val httpUrl = decision.sanitizedUrl.toHttpUrlOrNull() ?: return null
        if (!request.method.equals("GET", ignoreCase = true) && !request.method.equals("HEAD", ignoreCase = true)) {
            return null
        }

        val okHttpRequest = okhttp3.Request.Builder()
            .url(httpUrl)
            .headers(SecurityHeaderFactory.buildRequestHeaders(request.requestHeaders, httpUrl, profile, topLevelHost, policy))
            .method(request.method, null)
            .build()

        var response: okhttp3.Response? = null
        return try {
            val startTime = System.currentTimeMillis()
            response = DnsManager.secureClient(policy.networkBlockIpv6).newCall(okHttpRequest).execute()
            val duration = System.currentTimeMillis() - startTime
            AmnosLog.d("NetworkFetcher", "Proxied response [${response.code}] in ${duration}ms: $httpUrl")

            if (!response.isSuccessful) {
                AmnosLog.w("NetworkFetcher", "Proxied fetch REJECTED: HTTP ${response.code} for $httpUrl")
                response.close()
                return null
            }

            val body = response.body ?: run {
                response.close()
                return null
            }

            val contentType = body.contentType()
            val mimeType = if (contentType != null) "${contentType.type}/${contentType.subtype}" else "text/html"
            val charset = contentType?.charset(Charsets.UTF_8)?.name() ?: "UTF-8"

            WebResourceResponse(
                mimeType,
                charset,
                response.code,
                response.message.ifBlank { "OK" },
                SecurityHeaderFactory.buildResponseHeaders(response, decision.kind, policy),
                ResilientInputStream(body.byteStream(), httpUrl.toString())
            )
        } catch (e: Exception) {
            AmnosLog.e("NetworkFetcher", "Proxied fetch CRITICAL FAILURE: ${e.javaClass.simpleName} (${e.message}) for $httpUrl")
            response?.close()
            null
        }
    }
}

/**
 * A wrapper InputStream that catches SocketTimeoutExceptions and returns EOF instead.
 */
private class ResilientInputStream(
    private val inner: java.io.InputStream,
    private val url: String
) : java.io.InputStream() {
    override fun read(): Int = try { inner.read() } catch (e: Exception) { -1 }
    override fun read(b: ByteArray): Int = read(b, 0, b.size)
    override fun read(b: ByteArray, off: Int, len: Int): Int = try {
        inner.read(b, off, len)
    } catch (e: Exception) {
        -1
    }
    override fun close() = try { inner.close() } catch (e: Exception) { }
    override fun available(): Int = try { inner.available() } catch (e: Exception) { 0 }
}
