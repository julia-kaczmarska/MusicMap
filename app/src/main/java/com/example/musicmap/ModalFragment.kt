package com.example.musicmap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ModalFragment : DialogFragment() {

    private var spotifyUri: String? = null
    private var accessToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            spotifyUri = it.getString("spotifyUri")
            accessToken = it.getString("accessToken")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.modal_fragment, container, false)

        val titleTextView: TextView = view.findViewById(R.id.songTitle)
        val artistTextView: TextView = view.findViewById(R.id.songArtist)
        val albumImageView: ImageView = view.findViewById(R.id.albumImage)
        val playButton: Button = view.findViewById(R.id.playButton)

        playButton.setOnClickListener {
            spotifyUri?.let { uri ->
                playSongOnSpotify(uri)
            }
        }

        spotifyUri?.let { uri ->
            fetchTrackDetails(uri, titleTextView, artistTextView, albumImageView)
        }

        view.findViewById<View>(R.id.rootLayout).setOnClickListener {
            dismiss()
        }

        return view
    }

    private fun fetchTrackDetails(spotifyUri: String, titleView: TextView, artistView: TextView, imageView: ImageView) {
        accessToken?.let { token ->
            val trackId = spotifyUri.split(":").last()
            val url = "https://api.spotify.com/v1/tracks/$trackId"

            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()

            Log.d("ModalFragment", "Fetching track details from: $url with token: $token")

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("ModalFragment", "Failed to fetch track details: ${e.message}")
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Failed to load track details", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        response.body?.string()?.let { responseBody ->
                            val json = JSONObject(responseBody)
                            val title = json.getString("name")
                            val artist = json.getJSONArray("artists").getJSONObject(0).getString("name")
                            val albumImageUrl = json.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url")

                            activity?.runOnUiThread {
                                titleView.text = title
                                artistView.text = artist
                                Glide.with(this@ModalFragment).load(albumImageUrl).into(imageView)
                            }
                        }
                    } else {
                        response.body?.string()?.let { responseBody ->
                            Log.e("ModalFragment", "Failed to fetch track details: ${response.message}, response body: $responseBody")
                        } ?: Log.e("ModalFragment", "Failed to fetch track details: ${response.message}")
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Failed to load track details", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } ?: run {
            Log.e("ModalFragment", "Access token is null")
        }
    }

    private fun playSongOnSpotify(spotifyUri: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUri))
        intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://${requireContext().packageName}"))
        startActivity(intent)
    }

    companion object {
        @JvmStatic
        fun newInstance(spotifyUri: String, accessToken: String) =
            ModalFragment().apply {
                arguments = Bundle().apply {
                    putString("spotifyUri", spotifyUri)
                    putString("accessToken", accessToken)
                }
            }
    }
}
