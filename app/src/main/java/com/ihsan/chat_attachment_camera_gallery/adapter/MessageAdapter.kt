package com.ihsan.chat_attachment_camera_gallery.adapter

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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
import com.ihsan.chat_attachment_camera_gallery.utils.MyApplication

class MessageAdapter(private val messageList: List<Message>) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
    private val TAG = "MessageAdapter"

    class ViewHolder(private val binding: View) : RecyclerView.ViewHolder(binding) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val playImageView: ImageView = itemView.findViewById(R.id.play_imageView)
        val videoView: VideoView = itemView.findViewById(R.id.videoPlayer)
        val messageCard: CardView = itemView.findViewById(R.id.message_card)
        val relativelayout: RelativeLayout = itemView.findViewById(R.id.relativeLayoutMedia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val videoView = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_message_item, parent, false)
        return ViewHolder(videoView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messageList[position]
        val messageCardView = holder.messageCard
        val videoView = holder.videoView
        val imageView = holder.imageView
        val playImageView = holder.playImageView
        val textView = holder.textView
        val relativeLayout = holder.relativelayout

        //android:foreground="@drawable/ic_play_circle_outline"
        Log.d(TAG, "onBindViewHolder: $message")

        //changing massage align right if it is sender
        if (message.isSender == true) {
            val params = messageCardView.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
            messageCardView.layoutParams = params
            messageCardView.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    MyApplication.instance,
                    R.color.sender_background_color
                )
            )
        }



        //changing UI based in message type and loading message
        when (message.messageType) {
            //for video message
            MessageType.VIDEO -> {
                playImageView.visibility = ViewGroup.VISIBLE
                /*relativeLayout.visibility = ViewGroup.VISIBLE
                imageView.visibility = ViewGroup.VISIBLE
                videoView.visibility = ViewGroup.GONE
                textView.visibility = ViewGroup.GONE
                textView.text = null*/

                //click listener for video media item
                messageCardView.setOnClickListener {
                    if (videoView.isPlaying) {
                        //showing video thumbnail again
                        videoView.setVideoURI(null)
                        imageView.visibility = ViewGroup.VISIBLE
                        playImageView.visibility = ViewGroup.VISIBLE
                        videoView.visibility = ViewGroup.INVISIBLE
                    } else {
                        videoView.visibility = ViewGroup.VISIBLE
                        //parsing video
                        val videoUri = Uri.parse(message.data)
                        videoView.setVideoURI(videoUri)
                        videoView.setOnPreparedListener {
                            videoView.start()
                            imageView.visibility = ViewGroup.GONE
                            playImageView.visibility = ViewGroup.GONE
                        }
                        videoView.setOnCompletionListener {
                            videoView.setVideoURI(null)
                            imageView.visibility = ViewGroup.VISIBLE
                            playImageView.visibility = ViewGroup.VISIBLE
                            videoView.visibility = ViewGroup.GONE
                        }
                    }
                }

                //navigating to ExoPlayer Fragment on Long click listener on a video message item
                messageCardView.setOnLongClickListener {
                    val action =
                        BottomAttachmentOptionFragmentDirections.actionBottomAttachmentOptionFragmentToExoPlayerFragment(
                            BackState.HOME, message.data, true
                        )
                    Navigation.findNavController(holder.itemView).navigate(action)
                    return@setOnLongClickListener true
                }
            }

            //for image message
            MessageType.IMAGE -> {
                videoView.visibility = ViewGroup.GONE
                playImageView.visibility = ViewGroup.GONE

                messageCardView.setOnClickListener {
                    //Navigate to Preview and Crop
                    val action =
                        BottomAttachmentOptionFragmentDirections.actionBottomAttachmentOptionFragmentToCameraPreviewFragment(
                            message.data, BackState.HOME
                        )
                    Navigation.findNavController(it).navigate(action)
                }
            }
            //for text message
            MessageType.TEXT -> {
                textView.visibility = ViewGroup.VISIBLE

                imageView.visibility = ViewGroup.GONE
                playImageView.visibility = ViewGroup.GONE
                videoView.visibility = ViewGroup.GONE
                relativeLayout.visibility = ViewGroup.GONE
                textView.text = message.data
            }
        }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        val message = messageList[holder.bindingAdapterPosition]
        val relativeLayout= holder.relativelayout
        val imageView = holder.imageView
        val videoView = holder.videoView
        val textView = holder.textView
        //changing media layout visibility and shape based on media shape
        if (message.messageType == MessageType.VIDEO || message.messageType == MessageType.IMAGE) {
            relativeLayout.visibility = ViewGroup.VISIBLE
            imageView.visibility = ViewGroup.VISIBLE
            //playImageView.visibility = ViewGroup.VISIBLE

            videoView.visibility = ViewGroup.INVISIBLE
            textView.visibility = ViewGroup.GONE

            //loading video thumbnail image using glide
            loadResizedLayoutImageWithGlide(message.data, imageView, relativeLayout)
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        Log.d(TAG, "bindingAdapterPosition: ${holder.bindingAdapterPosition}")
        if (messageList[holder.bindingAdapterPosition].messageType == MessageType.VIDEO && holder.videoView.isPlaying) {
            holder.playImageView.visibility = ViewGroup.VISIBLE
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.imageView.setImageBitmap(null)
        holder.videoView.stopPlayback()
        holder.videoView.setVideoURI(null)
    }

    private fun loadResizedLayoutImageWithGlide(
        imageData: String,
        imageView: ImageView,
        relativeLayout: RelativeLayout
    ) {
        var contentWidth: Float
        //load thumbnail for the video
        Glide.with(MyApplication.instance)
            .asBitmap()
            .load(imageData)
            .placeholder(R.drawable.ic_loading) // Optional placeholder image
            .error(R.drawable.ic_cancel) // Optional error image
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    //ratio of the video bitmap
                    val bitmapRatio = resource.width.toFloat()/resource.height.toFloat()
                    //getting the content height for the video
                    contentWidth = (imageView.height * bitmapRatio)

                    if (contentWidth > 900) {
                        contentWidth *= .8f
                        val contentHeight = relativeLayout.layoutParams.height * .8f

                        relativeLayout.layoutParams.height = contentHeight.toInt()
                        relativeLayout.layoutParams.width = contentWidth.toInt()
                        Log.d(TAG, "onResourceReady: height:${contentHeight} width:$contentWidth")
                    }else{
                        relativeLayout.layoutParams.width = contentWidth.toInt()
                    }


                    imageView.setImageBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    relativeLayout.visibility = ViewGroup.GONE
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {}
            })
    }

    override fun getItemCount(): Int {
        return messageList.size
    }
}