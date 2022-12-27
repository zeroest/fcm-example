package me.zeroest.pushserver.model

data class PushNotificationResponse(
    private var status: Int = 0,
    private var message: String,
)