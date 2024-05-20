package com.example.musicmap
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.Call

object RetrofitClient {
    private const val BASE_URL = "http://192.168.0.88:3000/"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: Api by lazy {
        retrofit.create(Api::class.java)
    }
}

interface Api {
    @GET("markers")
    fun getMarkers(): Call<List<MarkerEntity>>
}

