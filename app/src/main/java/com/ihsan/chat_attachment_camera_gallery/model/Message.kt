package com.ihsan.chat_attachment_camera_gallery.model

data class Message(
    val messageType: MessageType,
    var isSender:Boolean?,
    val senderId: String,
    val receiverId: String,
    val data:String, //data of message like text, image Uri and video Uri in string
    val timestamp: Long,
    var isRead: Boolean
)
