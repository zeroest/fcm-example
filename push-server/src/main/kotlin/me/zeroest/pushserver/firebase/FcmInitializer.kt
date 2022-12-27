package me.zeroest.pushserver.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class FcmInitializer {

    @Value("\${app.firebase-service-account-file}")
    private val serviceAccountPath: String? = null

    var logger = LoggerFactory.getLogger(FcmInitializer::class.java)

    @PostConstruct
    fun initialize() {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(ClassPathResource(serviceAccountPath!!).inputStream))
            .build()

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
            logger.info("Firebase application has been initialized")
        }
    }

}