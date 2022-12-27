package me.zeroest.pushserver.controller

import me.zeroest.pushserver.model.PushNotificationRequest
import me.zeroest.pushserver.model.PushNotificationResponse
import me.zeroest.pushserver.service.PushNotificationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PushNotificationController(
    private var pushNotificationService: PushNotificationService
) {

    @PostMapping("/notification/topic")
    fun sendNotification(@RequestBody request: PushNotificationRequest): ResponseEntity<PushNotificationResponse> {
        pushNotificationService.sendPushNotificationWithoutData(request)
        return ResponseEntity<PushNotificationResponse>(
            PushNotificationResponse(HttpStatus.OK.value(), "Notification has been sent."),
            HttpStatus.OK
        )
    }

    @PostMapping("/notification/token")
    fun sendTokenNotification(@RequestBody request: PushNotificationRequest): ResponseEntity<PushNotificationResponse> {
        pushNotificationService.sendPushNotificationToToken(request)
        return ResponseEntity<PushNotificationResponse>(
            PushNotificationResponse(HttpStatus.OK.value(), "Notification has been sent."),
            HttpStatus.OK
        )
    }

    @PostMapping("/notification/data")
    fun sendDataNotification(@RequestBody request: PushNotificationRequest): ResponseEntity<PushNotificationResponse> {
        pushNotificationService.sendPushNotification(request)
        return ResponseEntity<PushNotificationResponse>(
            PushNotificationResponse(HttpStatus.OK.value(), "Notification has been sent."),
            HttpStatus.OK
        )
    }

    @GetMapping("/notification")
    fun sendSampleNotification(): ResponseEntity<PushNotificationResponse> {
        pushNotificationService.sendSamplePushNotification()
        return ResponseEntity<PushNotificationResponse>(
            PushNotificationResponse(HttpStatus.OK.value(), "Notification has been sent."),
            HttpStatus.OK
        )
    }
}