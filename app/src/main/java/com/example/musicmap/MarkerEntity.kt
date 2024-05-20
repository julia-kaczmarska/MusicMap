package com.example.musicmap

data class MarkerEntity (
    val id: Int,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val spotifyUri: String
)