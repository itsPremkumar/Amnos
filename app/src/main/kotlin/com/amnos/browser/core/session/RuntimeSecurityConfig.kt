package com.amnos.browser.core.session

import java.util.UUID

object RuntimeSecurityConfig {
    val webViewProfileSuffix: String by lazy {
        "amnos_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    }
}
