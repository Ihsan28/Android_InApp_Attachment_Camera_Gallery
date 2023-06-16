package com.ihsan.chat_attachment_camera_gallery.adapter

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
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
import com.ihsan.chat_attachment_camera_gallery.utils.MyApplication
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class MessageAdapterV3(private val messageList: List<Message>) :
    RecyclerView.Adapter<MessageAdapterV3.ViewHolder>() {
    private val TAG = "MessageAdapter"

    inner class ViewHolder(private val binding: View) : RecyclerView.ViewHolder(binding) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val playImageView: ImageView = itemView.findViewById(R.id.play_imageView)
        val videoView: VideoView = itemView.findViewById(R.id.videoPlayer)
        val messageCard: CardView = itemView.findViewById(R.id.message_card)
        val mediaRelativeLayout: RelativeLayout = itemView.findViewById(R.id.relativeLayoutMedia)
        var contentHeight = 300

        @OptIn(DelicateCoroutinesApi::class)
        fun bind(videoUri: Uri) {
            // Load the video thumbnail asynchronously
            GlobalScope.launch(Dispatchers.Main) {
                val thumbnailBitmap = getVideoThumbnail(videoUri)
                var ratio= 1f
                if (thumbnailBitmap != null) {
                    ratio= thumbnailBitmap.height.toFloat() /thumbnailBitmap.width.toFloat()

                    mediaRelativeLayout.layoutParams.height = (contentHeight * ratio).toInt()
                    mediaRelativeLayout.layoutParams.width= contentHeight
                    //set image with glide
                    Glide.with(MyApplication.instance)
                        .load(thumbnailBitmap)
                        .into(imageView)
                    //imageView.setImageBitmap(thumbnailBitmap)
                }
            }
        }
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
        val mediaRelativeLayout = holder.mediaRelativeLayout


        //android:foreground="@drawable/ic_play_circle_outline"
        Log.d(TAG, "onBindViewHolder: $message")
        Log.d(TAG, "onResourceReady: $position")

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
                imageView.visibility = ViewGroup.VISIBLE
                playImageView.visibility = ViewGroup.VISIBLE
                //textView.visibility = ViewGroup.GONE
                textView.visibility = ViewGroup.VISIBLE
                videoView.visibility = ViewGroup.GONE
                //textView.text = null
                textView.text = position.toString()

                //holder.bind(Uri.parse(message.data))

                /*holder.contentHeight=100
                //load thumbnail for the video
                Glide.with(MyApplication.instance)
                    .asBitmap()
                    .load(message.data)
                    .placeholder(R.drawable.ic_loading) // Optional placeholder image
                    .error(R.drawable.ic_cancel) // Optional error image
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            //ratio of the video bitmap
                            val bitmapRatio = resource.height.toFloat() / resource.width.toFloat()
                            //getting the content height for the video
                            holder.contentHeight=(imageView.width * bitmapRatio).toInt()
                            Log.d(TAG, "onResourceReady:image width-> ${imageView.width} content height: ${holder.contentHeight} contentRatio: $bitmapRatio")
                            imageView.layoutParams.height=holder.contentHeight
                            playImageView.layoutParams.height=holder.contentHeight
                            imageView.setImageBitmap(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            imageView.visibility = ViewGroup.GONE
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {}
                    })

                //click listener for video media item
                messageCardView.setOnClickListener {
                    if (videoView.isPlaying) {
                        //showing video thumbnail again
                        videoView.setVideoURI(null)
                        imageView.visibility = ViewGroup.VISIBLE
                        playImageView.visibility = ViewGroup.VISIBLE
                        videoView.visibility = ViewGroup.GONE
                    } else {
                        videoView.visibility = ViewGroup.VISIBLE
                        //setting custom height
                        videoView.layoutParams.height=holder.contentHeight
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
                            BackState.HOME,message.data, true
                        )
                    Navigation.findNavController(holder.itemView).navigate(action)
                    return@setOnLongClickListener true
                }*/
            }

            //for image message
            MessageType.IMAGE -> {
                imageView.visibility = ViewGroup.VISIBLE
                //textView.visibility = ViewGroup.GONE
                textView.visibility = ViewGroup.VISIBLE
                playImageView.visibility = ViewGroup.GONE
                videoView.visibility = ViewGroup.GONE
                //textView.text = null
                textView.text = position.toString()
                imageView.foreground = null

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
                imageView.foreground = null
                imageView.background = null
                videoView.setVideoURI(null)
                textView.text = message.data
            }
        }

        if (message.messageType == MessageType.IMAGE) {
            //load thumbnail for the video
            Glide.with(MyApplication.instance)
                .asBitmap()
                .load(message.data)
                .placeholder(R.drawable.ic_loading) // Optional placeholder image
                .error(R.drawable.ic_cancel) // Optional error image
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val layoutParams = imageView.layoutParams
                        val bitmapWidth = resource.width
                        val bitmapHeight = resource.height
                        val bitmapRatio = bitmapHeight.toFloat() / bitmapWidth.toFloat()

                        layoutParams.height = (imageView.width * bitmapRatio).toInt()
                        imageView.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        imageView.visibility = ViewGroup.GONE
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {}
                })
        }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (messageList[holder.bindingAdapterPosition].messageType == MessageType.VIDEO || messageList[holder.bindingAdapterPosition].messageType == MessageType.IMAGE) {
            holder.mediaRelativeLayout.visibility = ViewGroup.VISIBLE
            if (messageList[holder.bindingAdapterPosition].messageType == MessageType.VIDEO){
                holder.bind(messageList[holder.bindingAdapterPosition].data.toUri())
            }

        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        Log.d(TAG, "bindingAdapterPosition: ${holder.bindingAdapterPosition}")
        holder.mediaRelativeLayout.visibility = ViewGroup.GONE
        if (messageList[holder.bindingAdapterPosition].messageType == MessageType.VIDEO && holder.videoView.isPlaying) {
            holder.videoView.setVideoURI(null)
            holder.imageView.visibility = ViewGroup.VISIBLE
            holder.playImageView.visibility = ViewGroup.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    private suspend fun getVideoThumbnail(videoUri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            val retriever = MediaMetadataRetriever()

            try {
                retriever.setDataSource(MyApplication.instance, videoUri)

                // Retrieve the first frame of the video
                bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

                // Resize the bitmap if needed (e.g., to fit ImageView dimensions)
                // bitmap = resizeBitmap(bitmap, desiredWidth, desiredHeight)

            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }

            bitmap
        }
    }
}