package com.example.musicmap

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class ModalFragment : DialogFragment() {

    private var songTitle: String? = null
    private var spotifyUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            songTitle = it.getString("songTitle")
            spotifyUri = it.getString("spotifyUri")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.modal_fragment, container, false)

        val titleTextView: TextView = view.findViewById(R.id.songTitle)
        titleTextView.text = songTitle

        val playButton: Button = view.findViewById(R.id.playButton)
        val addToQueueButton: Button = view.findViewById(R.id.addToQueueButton)

        playButton.setOnClickListener {
            spotifyUri?.let { uri ->
                playSongOnSpotify(uri)
            }
        }

        addToQueueButton.setOnClickListener {
            spotifyUri?.let { uri ->
                addToQueueOnSpotify(uri)
            }
        }

        view.findViewById<View>(R.id.rootLayout).setOnClickListener {
            dismiss()
        }

        return view
    }

    private fun playSongOnSpotify(spotifyUri: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUri))
        intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://${requireContext().packageName}"))
        startActivity(intent)
    }

    private fun addToQueueOnSpotify(spotifyUri: String) {
        // Add your logic to add the song to the Spotify queue here
    }

    companion object {
        @JvmStatic
        fun newInstance(songTitle: String, spotifyUri: String) =
            ModalFragment().apply {
                arguments = Bundle().apply {
                    putString("songTitle", songTitle)
                    putString("spotifyUri", spotifyUri)
                }
            }
    }
}
