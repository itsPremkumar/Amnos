package com.amnos.browser.core.network

import com.amnos.browser.core.session.AmnosLog
import java.io.IOException
import java.net.*

/**
 * AmnosProxySelector: The network jail.
 * Forces all JVM-level network requests through the loopback proxy sandbox.
 */
class AmnosProxySelector(
    private val proxyHost: String = "127.0.0.1",
    private val proxyPort: Int,
    private val delegate: ProxySelector? = ProxySelector.getDefault()
) : ProxySelector() {

    override fun select(uri: URI?): List<Proxy> {
        val destination = uri?.host ?: "unknown"
        
        // Block all non-loopback traffic at the JVM level unless it is to our proxy
        if (destination == "localhost" || destination == "127.0.0.1") {
            return listOf(Proxy.NO_PROXY)
        }

        AmnosLog.d("ProxySelector", "SANDBOX ENFORCED: Routing request to $destination through proxy.")
        
        return listOf(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort)))
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        AmnosLog.e("ProxySelector", "Sandbox Proxy Connection Failed for $uri")
        delegate?.connectFailed(uri, sa, ioe)
    }

    companion object {
        fun apply(proxyPort: Int) {
            try {
                ProxySelector.setDefault(AmnosProxySelector(proxyPort = proxyPort))
                AmnosLog.i("ProxySelector", "System Proxy Lock Engaged (Port: $proxyPort)")
            } catch (e: Exception) {
                AmnosLog.e("ProxySelector", "Failed to engage System Proxy Lock", e)
            }
        }
    }
}
