package com.amnos.browser.ui.screens.browser.layouts

object AmnosLayouts {
    fun blockedPageHtml(reason: String): String = """
        <!doctype html>
        <html lang="en">
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Blocked by Amnos</title>
            <style>
                body { background:#0d1117; color:#f8fafc; font-family:sans-serif; padding:24px; }
                .card { max-width:560px; margin:48px auto; background:#161b22; border:1px solid #223; border-radius:16px; padding:24px; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1); }
                h1 { margin:0 0 12px; font-size:24px; color: #f8fafc; }
                p { color:#94a3b8; line-height:1.5; margin-bottom: 20px; }
                .reason-badge { background: #fee2e2; color: #991b1b; padding: 4px 12px; border-radius: 9999px; font-size: 12px; font-weight: 600; display: inline-block; font-family: monospace; }
                .footer { margin-top: 32px; border-top: 1px solid #30363d; padding-top: 16px; font-size: 12px; color: #484f58; text-align: center; }
            </style>
        </head>
        <body>
            <div class="card">
                <h1>Request blocked</h1>
                <p>Amnos stopped this request because it violated the active privacy policy. This prevents trackers from building a profile of your activity.</p>
                <div class="reason-badge">POLICY_VIOLATION: ${reason}</div>
                <div class="footer">Protected by Amnos Ghost Engine</div>
            </div>
        </body>
        </html>
    """.trimIndent()
}
