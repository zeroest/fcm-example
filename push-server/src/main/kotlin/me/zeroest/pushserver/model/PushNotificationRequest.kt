package me.zeroest.pushserver.model

data class PushNotificationRequest(
    var title: String,
    var message: String,
    var topic: String,
) {
    constructor(
        title: String,
        message: String,
        topic: String,
        token: String,
    ) : this(title = title, message = message, topic = topic)

    var token: String? = null
}