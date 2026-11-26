package com.example.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface GoogleDriveUploadApi {
    @Multipart
    @POST("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
    suspend fun uploadToAppDataFolder(
        @Header("Authorization") bearerToken: String,
        @Part("metadata") metadata: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<Unit>
}
