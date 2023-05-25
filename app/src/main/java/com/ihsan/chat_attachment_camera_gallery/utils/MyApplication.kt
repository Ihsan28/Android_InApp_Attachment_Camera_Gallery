package com.ihsan.chat_attachment_camera_gallery.utils

import android.app.Application

class MyApplication:Application() {
    companion object {
        lateinit var instance: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}