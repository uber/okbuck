package com.uber.okbuck.example

class ATestNoopAnalytics: Analytics {

    override fun sendAnalyticsEvent(event: String, content: String) {
        // Do nothing
    }

    override fun getSentEvents(): Map<String, String> = mapOf()
}
