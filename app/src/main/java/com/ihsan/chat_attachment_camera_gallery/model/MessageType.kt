package com.ihsan.chat_attachment_camera_gallery.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class MessageType : Parcelable {
    TEXT, IMAGE, VIDEO
}