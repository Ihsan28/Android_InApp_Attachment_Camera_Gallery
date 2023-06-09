package com.ihsan.chat_attachment_camera_gallery.adapter.videoplayeradapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.ihsan.chat_attachment_camera_gallery.R
import com.ihsan.chat_attachment_camera_gallery.model.Message
import com.ihsan.chat_attachment_camera_gallery.utils.customrecyclerview.VideoPlayerViewHolder

class VideoPlayerRecyclerAdapter(private val mediaObjects: ArrayList<Message>, private val requestManager: RequestManager) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_video_list_item, parent, false)
        return VideoPlayerViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        (viewHolder as VideoPlayerViewHolder).onBind(mediaObjects[position], requestManager)
    }

    override fun getItemCount(): Int {
        return mediaObjects.size
    }
}