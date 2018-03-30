package com.uber.okbuck.example

class TestAnalyticsImpl : Analytics {

    override fun sendAnalyticsEvent(event: String, content: String) {
        // Do nothing
    }

    override fun getSentEvents(): Map<String, String> = mapOf()
}
