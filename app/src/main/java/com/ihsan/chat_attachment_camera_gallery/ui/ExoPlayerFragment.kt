package com.ihsan.chat_attachment_camera_gallery.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.ihsan.chat_attachment_camera_gallery.R
import com.ihsan.chat_attachment_camera_gallery.databinding.FragmentExoPlayerBinding
import com.ihsan.chat_attachment_camera_gallery.model.BackState
import com.ihsan.chat_attachment_camera_gallery.model.MessageType

class ExoPlayerFragment : Fragment() {
    private lateinit var binding: FragmentExoPlayerBinding
    private val args: ExoPlayerFragmentArgs by navArgs()
    private lateinit var player: ExoPlayer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity).supportActionBar?.hide()
        // Inflate the layout for this fragment
        binding = FragmentExoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val videoUriString = args.uriString
        val backTo = args.backTo
        val playerView = binding.exoPlayer
        val saveVideo = binding.saveVideo
        val discardVideo = binding.discardVideo

        // Create a media source
        val mediaItem = MediaItem.fromUri(Uri.parse(videoUriString))
        val mediaSource =
            ProgressiveMediaSource.Factory(DefaultDataSource.Factory(requireContext()))
                .createMediaSource(mediaItem)

        // Initialize the player
        player = ExoPlayer.Builder(requireContext()).build()
        playerView.player = player
        playerView.setShowNextButton(false)
        playerView.setShowPreviousButton(false)

        // Prepare the player with the media source
        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true

        //checking the fragment using only for view or it's a preview after recording
        if (args.onlyPreview) {
            saveVideo.visibility = ViewGroup.GONE
            discardVideo.visibility = ViewGroup.GONE
        }

        saveVideo.setOnClickListener {
            player.stop()
            player.release()
            navigateToBottomHomeWithArgument(it, videoUriString)
        }

        discardVideo.setOnClickListener {
            player.stop()
            player.release()
            videoUriString.toUri().toFile().delete()
            when(backTo){
                BackState.HOME->navigateBackToHome(view)
                BackState.CAMERA->Navigation.findNavController(it)
                    .navigate(R.id.action_exoPlayerFragment_to_cameraFragment)
                BackState.VIDEO->Navigation.findNavController(it)
                    .navigate(R.id.action_exoPlayerFragment_to_videoViewFragment)
            }
        }

        // handling back press event
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when(backTo){
                        BackState.HOME->navigateBackToHome(view)
                        BackState.CAMERA->Navigation.findNavController(view)
                            .navigate(R.id.action_exoPlayerFragment_to_cameraFragment)
                        BackState.VIDEO->Navigation.findNavController(view)
                            .navigate(R.id.action_exoPlayerFragment_to_videoViewFragment)
                    }
                }
            })
    }

    private fun navigateBackToHome(view: View){
        (activity as AppCompatActivity).supportActionBar?.show()
        Navigation.findNavController(view)
            .navigate(R.id.action_exoPlayerFragment_to_bottomAttachmentOptionFragment)
    }

    private fun navigateToBottomHomeWithArgument(
        view: View,
        uri: String
    ) {
        (activity as AppCompatActivity).supportActionBar?.show()
        val action =
            ExoPlayerFragmentDirections.actionExoPlayerFragmentToBottomAttachmentOptionFragment(
                MessageType.VIDEO,
                uri
            )
        Navigation.findNavController(view).navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.release()
    }
}