package com.uber.okbuck.example

class AnalyticsImpl : Analytics {

    private val sentEvents: MutableMap<String, String> = mutableMapOf()

    override fun sendAnalyticsEvent(event: String, content: String) {
        sentEvents[event] = content
    }

    override fun getSentEvents() : Map<String, String> = sentEvents.toMap()
}
