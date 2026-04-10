package com.privacy.browser.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object UrlSanitizer {
    private val trackingParameters = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid",
        "gclid", "wbraid", "gbraid",
        "msclkid",
        "mc_eid",
        "yclid",
        "_hsenc", "_hsmi",
        "mkt_tok"
    )

    fun sanitize(url: String): String {
        val httpUrl = url.toHttpUrlOrNull() ?: return url
        if (httpUrl.querySize == 0) {
            return url
        }

        val builder = httpUrl.newBuilder().query(null)
        for (index in 0 until httpUrl.querySize) {
            val name = httpUrl.queryParameterName(index)
            val value = httpUrl.queryParameterValue(index)
            if (!trackingParameters.contains(name.lowercase())) {
                builder.addQueryParameter(name, value)
            }
        }

        return builder.build().toString()
    }
}
