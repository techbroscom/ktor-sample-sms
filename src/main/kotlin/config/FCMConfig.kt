package com.example.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import java.io.FileInputStream

object FCMConfig {
    fun initialize() {
        try {
            // Initialize Firebase Admin SDK
            val serviceAccount = javaClass.getResourceAsStream("/sms-flutter-c6d80-firebase-adminsdk-fbsvc-64ff35bbb5.json")
                ?: throw IllegalStateException("Firebase service account file not found in resources")

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
            }

            println("Firebase Admin SDK initialized successfully")
        } catch (e: Exception) {
            println("Error initializing Firebase: ${e.message}")
        }
    }

    fun getMessaging(): FirebaseMessaging {
        return FirebaseMessaging.getInstance()
    }
}