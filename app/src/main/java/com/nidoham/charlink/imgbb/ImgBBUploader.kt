package com.nidoham.charlink.imgbb

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream

object ImgBBUploader {

    private const val API_KEY = "9e667bec4ecd933fd3994a630e38de1f" // Ideally, put this in local.properties or BuildConfig
    private const val UPLOAD_URL = "https://api.imgbb.com/1/upload"

    private val client = OkHttpClient()
    private val gson = Gson()

    /**
     * Uploads an image to ImgBB using Coroutines.
     * Returns a Result<String> containing the Display URL or an Error.
     */
    suspend fun uploadImage(context: Context, uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Convert Uri to Base64 (Heavy operation done on IO thread)
                val base64Image = getBase64FromUri(context, uri)
                    ?: return@withContext Result.failure(Exception("Failed to process image file"))

                // 2. Build Request Body
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", API_KEY)
                    .addFormDataPart("image", base64Image)
                    .build()

                // 3. Build Request
                val request = Request.Builder()
                    .url(UPLOAD_URL)
                    .post(requestBody)
                    .build()

                // 4. Execute Request
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val imgBBResponse = gson.fromJson(responseBody, ImgBBResponse::class.java)
                    if (imgBBResponse.success && imgBBResponse.data != null) {
                        Result.success(imgBBResponse.data.displayUrl)
                    } else {
                        Result.failure(Exception("ImgBB API returned success=false"))
                    }
                } else {
                    Result.failure(Exception("HTTP Error: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getBase64FromUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val byteBuffer = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var len: Int
            while (inputStream.read(buffer).also { len = it } != -1) {
                byteBuffer.write(buffer, 0, len)
            }
            inputStream.close()
            val imageBytes = byteBuffer.toByteArray()
            Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}