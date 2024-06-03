package com.example.musicmap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ModalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modal)

        val songTitle = intent.getStringExtra("songTitle")
        val spotifyUri = intent.getStringExtra("spotifyUri")

        val titleTextView: TextView = findViewById(R.id.songTitle)
        titleTextView.text = songTitle

        val playButton: Button = findViewById(R.id.playButton)
        val addToQueueButton: Button = findViewById(R.id.addToQueueButton)

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

//        findViewById<View>(R.id.rootLayout).setOnClickListener {
//            finish()
//        }
    }

    private fun playSongOnSpotify(spotifyUri: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUri))
        intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://${this.packageName}"))
        startActivity(intent)
    }

    private fun addToQueueOnSpotify(spotifyUri: String) {
        // Add your logic to add the song to the Spotify queue here
    }
}
