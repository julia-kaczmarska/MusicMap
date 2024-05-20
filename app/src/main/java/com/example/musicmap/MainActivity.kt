package com.example.musicmap

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import com.example.musicmap.ui.theme.MusicMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Znajdź przycisk i ustaw onClickListener
        val button: Button = findViewById(R.id.but1)
        button.setOnClickListener {
            // Utwórz intent, aby uruchomić MapsActivity
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
    }
}

