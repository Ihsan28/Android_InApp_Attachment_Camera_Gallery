package com.ihsan.chat_attachment_camera_gallery.adapter

import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.ihsan.chat_attachment_camera_gallery.R
import com.ihsan.chat_attachment_camera_gallery.model.BackState
import com.ihsan.chat_attachment_camera_gallery.model.Message
import com.ihsan.chat_attachment_camera_gallery.model.MessageType
import com.ihsan.chat_attachment_camera_gallery.ui.BottomAttachmentOptionFragmentDirections

class MessageAdapterV2(private val messageList: List<Message>) :
    RecyclerView.Adapter<MessageAdapterV2.ViewHolder>() {
    private val TAG = "MessageAdapter"

    class ViewHolder(private val binding: View) : RecyclerView.ViewHolder(binding) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val playImageView: ImageView = itemView.findViewById(R.id.play_imageView)
        val videoView: VideoView = itemView.findViewById(R.id.videoPlayer)
        val messageCard: CardView = itemView.findViewById(R.id.message_card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val videoView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_message_item, parent, false)
        return ViewHolder(videoView)
    }

    // onBindView holder which will init image view and video view and text view dynamically
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messageList[position]
        holder.textView.text = message.data
        holder.messageCard.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
        when (message.messageType) {
            MessageType.TEXT -> {
                holder.imageView.visibility = ViewGroup.GONE
                holder.playImageView.visibility = ViewGroup.GONE
                holder.videoView.visibility = ViewGroup.GONE
                holder.textView.visibility = ViewGroup.VISIBLE
            }

            MessageType.IMAGE -> {
                holder.imageView.visibility = ViewGroup.VISIBLE
                holder.playImageView.visibility = ViewGroup.GONE
                holder.videoView.visibility = ViewGroup.GONE
                holder.textView.visibility = ViewGroup.GONE
                Glide.with(holder.imageView.context)
                    .asBitmap()
                    .load(message.data)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            holder.imageView.setImageBitmap(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })
            }

            MessageType.VIDEO -> {
                holder.imageView.visibility = ViewGroup.VISIBLE
                holder.playImageView.visibility = ViewGroup.VISIBLE
                holder.videoView.visibility = ViewGroup.GONE
                holder.textView.visibility = ViewGroup.GONE
                Glide.with(holder.imageView.context)
                    .asBitmap()
                    .load(message.data)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            holder.imageView.setImageBitmap(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })
            }
        }
        holder.imageView.setOnClickListener {
            if (message.messageType == MessageType.VIDEO) {
                holder.imageView.visibility = ViewGroup.GONE
                holder.playImageView.visibility = ViewGroup.GONE
                holder.videoView.visibility = ViewGroup.VISIBLE
                holder.videoView.setVideoURI(message.data.toUri())
                holder.videoView.start()
            }
        }
        holder.playImageView.setOnClickListener {
            if (message.messageType == MessageType.VIDEO) {
                holder.imageView.visibility = ViewGroup.GONE
                holder.playImageView.visibility = ViewGroup.GONE
                holder.videoView.visibility = ViewGroup.VISIBLE
                holder.videoView.setVideoURI(message.data.toUri())
                holder.videoView.start()
            }
        }
        holder.videoView.setOnCompletionListener {
            holder.videoView.setVideoURI(null)
            holder.imageView.visibility = ViewGroup.VISIBLE
            holder.playImageView.visibility = ViewGroup.VISIBLE
        }
        holder.videoView.setOnErrorListener { mp, what, extra ->
            holder.videoView.setVideoURI(null)
            holder.imageView.visibility = ViewGroup.VISIBLE
            holder.playImageView.visibility = ViewGroup.VISIBLE
            true
        }
        holder.messageCard.setOnClickListener {
            val action =
                BottomAttachmentOptionFragmentDirections.actionBottomAttachmentOptionFragmentToCameraPreviewFragment(
                    message.data,
                    BackState.HOME
                )
            Navigation.findNavController(it).navigate(action)
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        Log.d(TAG, "bindingAdapterPosition: ${holder.bindingAdapterPosition}")
        if (messageList[holder.bindingAdapterPosition].messageType == MessageType.VIDEO && holder.videoView.isPlaying) {
            holder.videoView.setVideoURI(null)
            holder.imageView.visibility = ViewGroup.VISIBLE
            holder.playImageView.visibility = ViewGroup.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }
}