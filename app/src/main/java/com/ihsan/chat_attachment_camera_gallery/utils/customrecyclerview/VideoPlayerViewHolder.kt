package com.ihsan.chat_attachment_camera_gallery.utils.customrecyclerview

import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.ihsan.chat_attachment_camera_gallery.R
import com.ihsan.chat_attachment_camera_gallery.model.Message
import java.io.File

class VideoPlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var media_container: FrameLayout
    var title: TextView
    var thumbnail: ImageView
    var volumeControl: ImageView
    var progressBar: ProgressBar
    var parent: View
    lateinit var requestManager: RequestManager

    init {
        parent = itemView
        media_container = itemView.findViewById(R.id.media_container)
        thumbnail = itemView.findViewById(R.id.thumbnail)
        title = itemView.findViewById(R.id.title)
        progressBar = itemView.findViewById(R.id.progressBar)
        volumeControl = itemView.findViewById(R.id.volume_control)
    }

    fun onBind(mediaObject: Message, requestManager: RequestManager) {
        this.requestManager = requestManager
        parent.tag = this
        title.text = mediaObject.messageType.toString()
        this.requestManager
            .asBitmap()
            .load(Uri.fromFile(File(mediaObject.data)))
            .into(thumbnail)
    }
}
