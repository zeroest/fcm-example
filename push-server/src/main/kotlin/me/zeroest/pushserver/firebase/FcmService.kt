package me.zeroest.pushserver.firebase

import com.google.firebase.messaging.*
import me.zeroest.pushserver.model.PushNotificationRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class FcmService {

    private val logger = LoggerFactory.getLogger(FcmService::class.java)

    fun sendMessage(data: Map<String, String>, request: PushNotificationRequest) {
        val message: Message = getPreconfiguredMessageWithData(data, request)
        val response = sendAndGetResponse(message)
        logger.info("Sent message with data. Topic: " + request.topic + ", " + response)
    }

    fun sendMessageWithoutData(request: PushNotificationRequest) {
        val message: Message = getPreconfiguredMessageWithoutData(request)
        val response = sendAndGetResponse(message)
        logger.info("Sent message without data. Topic: " + request.topic + ", " + response)
    }

    fun sendMessageToToken(request: PushNotificationRequest) {
        val message: Message = getPreconfiguredMessageToToken(request)
        val response = sendAndGetResponse(message)
        logger.info("Sent message to token. Device token: " + request.token + ", " + response)
    }

    private fun sendAndGetResponse(message: Message): String {
        return FirebaseMessaging
            .getInstance()
            .sendAsync(message)
            .get()
    }

    private fun getAndroidConfig(topic: String): AndroidConfig {
        val notification = AndroidNotification.builder().setSound(NotificationParameter.SOUND.value)
            .setColor(NotificationParameter.COLOR.value).setTag(topic).build()

        return AndroidConfig.builder()
            .setTtl(Duration.ofMinutes(2).toMillis())
            .setCollapseKey(topic)
            .setPriority(AndroidConfig.Priority.HIGH)
            .setNotification(notification)
            .build()
    }

    private fun getApnsConfig(topic: String): ApnsConfig {
        val aps = Aps.builder()
            .setCategory(topic)
            .setThreadId(topic)
            .build()

        return ApnsConfig.builder()
            .setAps(aps)
            .build()
    }

    private fun getPreconfiguredMessageToToken(request: PushNotificationRequest): Message {
        return getPreconfiguredMessageBuilder(request)
            .setToken(request.token)
            .build()
    }

    private fun getPreconfiguredMessageWithoutData(request: PushNotificationRequest): Message {
        return getPreconfiguredMessageBuilder(request)
            .setTopic(request.topic)
            .build()
    }

    private fun getPreconfiguredMessageWithData(data: Map<String, String>, request: PushNotificationRequest): Message {
        return getPreconfiguredMessageBuilder(request)
            .putAllData(data)
            .setTopic(request.topic)
            .build()
    }

    private fun getPreconfiguredMessageBuilder(request: PushNotificationRequest): Message.Builder {
        val androidConfig: AndroidConfig = getAndroidConfig(request.topic)
        val apnsConfig: ApnsConfig = getApnsConfig(request.topic)

        val notification = Notification.builder()
            .setTitle(request.title)
            .setBody(request.message)
            .build()

        return Message.builder()
            .setApnsConfig(apnsConfig)
            .setAndroidConfig(androidConfig)
            .setNotification(notification)
    }

}