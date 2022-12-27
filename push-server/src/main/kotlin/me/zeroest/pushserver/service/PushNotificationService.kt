package me.zeroest.pushserver.service

import me.zeroest.pushserver.firebase.FcmService
import me.zeroest.pushserver.model.PushNotificationRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ExecutionException

@Service
class PushNotificationService(
    @Value("#{\${app.notifications.defaults}}")
    private val defaults: Map<String, String>,
    private var fcmService: FcmService,
) {

    private val logger = LoggerFactory.getLogger(PushNotificationService::class.java)

    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    fun sendSamplePushNotification() {
        try {
            fcmService.sendMessageWithoutData(getSamplePushNotificationRequest())
        } catch (e: InterruptedException) {
            logger.error(e.message)
        } catch (e: ExecutionException) {
            logger.error(e.message)
        }
    }

    fun sendPushNotification(request: PushNotificationRequest) {
        try {
            fcmService.sendMessage(getSamplePayloadData(), request)
        } catch (e: InterruptedException) {
            logger.error(e.message)
        } catch (e: ExecutionException) {
            logger.error(e.message)
        }
    }

    fun sendPushNotificationWithoutData(request: PushNotificationRequest) {
        try {
            fcmService.sendMessageWithoutData(request)
        } catch (e: InterruptedException) {
            logger.error(e.message)
        } catch (e: ExecutionException) {
            logger.error(e.message)
        }
    }


    fun sendPushNotificationToToken(request: PushNotificationRequest) {
        try {
            fcmService.sendMessageToToken(request)
        } catch (e: InterruptedException) {
            logger.error(e.message)
        } catch (e: ExecutionException) {
            logger.error(e.message)
        }
    }


    private fun getSamplePayloadData(): Map<String, String> {
        val pushData: MutableMap<String, String> = HashMap()
        pushData["messageId"] = defaults["payloadMessageId"]!!
        pushData["text"] = defaults["payloadData"].toString() + " " + LocalDateTime.now()
        return pushData
    }


    private fun getSamplePushNotificationRequest(): PushNotificationRequest {
        return PushNotificationRequest(
            defaults["title"]!!,
            defaults["message"]!!,
            defaults["topic"]!!,
            defaults["token"]!!,
        )
    }
}