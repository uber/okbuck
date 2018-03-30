package com.uber.okbuck.example

interface Analytics {

    fun sendAnalyticsEvent(event: String, content: String = "")

    fun getSentEvents() : Map<String, String>
}
