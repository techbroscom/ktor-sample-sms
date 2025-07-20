package com.example.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import java.io.ByteArrayInputStream
import java.util.Base64

object FCMConfig {
    fun initialize() {
        try {
            /*val base64Key = System.getenv("FIREBASE_SERVICE_ACCOUNT")
                ?: throw IllegalStateException("FIREBASE_SERVICE_ACCOUNT env variable not set")

            val decodedKey = Base64.getDecoder().decode(base64Key)
            val serviceAccount = ByteArrayInputStream(decodedKey)*/

            val serviceAccount = javaClass.getResourceAsStream("/sms-flutter-c6d80-firebase-adminsdk-fbsvc-bcfb4007ff.json")
                ?: throw IllegalStateException("Firebase service account file not found in resources")

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println("Firebase Admin SDK initialized successfully")
            }
        } catch (e: Exception) {
            println("Error initializing Firebase: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getMessaging(): FirebaseMessaging {
        return FirebaseMessaging.getInstance()
    }
}
