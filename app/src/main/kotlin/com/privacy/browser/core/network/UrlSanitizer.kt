package com.privacy.browser.core.network

import android.net.Uri

object UrlSanitizer {
    // Inspired by Brave and DuckDuckGo
    private val trackingParameters = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", // Google Analytics
        "fbclid", // Facebook
        "gclid", "wbraid", "gbraid", // Google Ads
        "msclkid", // Bing Ads
        "mc_eid", // Mailchimp
        "yclid", // Yandex
        "_hsenc", "_hsmi", // HubSpot
        "mkt_tok" // Marketo
    )

    fun sanitize(url: String): String {
        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            return url
        }

        if (uri.isOpaque || uri.query == null) return url

        val builder = uri.buildUpon().clearQuery()
        var paramsRemoved = 0

        uri.queryParameterNames.forEach { name ->
            if (trackingParameters.contains(name.lowercase())) {
                paramsRemoved++
            } else {
                builder.appendQueryParameter(name, uri.getQueryParameter(name))
            }
        }

        return builder.build().toString()
    }
}
