package com.fahms.tagihjaringan

import android.app.Application
import com.fahms.tagihjaringan.data.local.UserPreferencesRepository
import com.fahms.tagihjaringan.data.repository.AppRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class TagihJaringanApplication : Application() {

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    lateinit var appRepository: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId("tagihjaringan")
                    .setApiKey("AIzaSyCZtBQ7hdR_Y-lQwRmLhMfyFDvcS_rACv4")
                    .setApplicationId("1:475246362649:android:66c6770c3d5db8e93baa88")
                    .setStorageBucket("tagihjaringan.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        userPreferencesRepository = UserPreferencesRepository(applicationContext)
        appRepository = AppRepository(userPreferencesRepository)
    }
}
