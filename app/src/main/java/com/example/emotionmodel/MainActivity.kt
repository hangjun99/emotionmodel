package com.example.emotionmodel

import android.content.Intent
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {
    private lateinit var selectedImage: ImageView
    private lateinit var uploadButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var emotionClassifier: EmotionClassifier
    private lateinit var buttonOpenWeb: Button
    private var topEmotion: String? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            displayImage(it)
            classifyEmotion(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectedImage = findViewById(R.id.selected_image)
        uploadButton = findViewById(R.id.upload_button)
        resultTextView = findViewById(R.id.result_text_view)
        emotionClassifier = EmotionClassifier(this)
        buttonOpenWeb = findViewById(R.id.button_open_web)

        uploadButton.setOnClickListener {
            getContent.launch("image/*")
        }

        buttonOpenWeb.setOnClickListener {
            topEmotion?.let {
                val url = getYouTubePlaylistUrl(it)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.setPackage(null) // 특정 앱을 지정하지 않음
                startActivity(intent)
            }
        }
    }

    private fun displayImage(uri: Uri) {
        Glide.with(this).load(uri).into(selectedImage)
    }

    private fun classifyEmotion(uri: Uri) {
        val bitmap = getBitmapFromUri(uri)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true) // 모델 입력 크기와 일치하도록 크기 조정
        val convertedBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true) // 비트맵 복사
        val results = emotionClassifier.classifyEmotion(convertedBitmap)

        val emotions = arrayOf("Angry", "Disgust", "Fear", "Happy", "Sad", "Surprise", "Neutral")
        val resultsText = emotions.zip(results.toTypedArray()).joinToString("\n") { "${it.first}: ${"%.2f".format(it.second * 100)}%" }

        resultTextView.text = resultsText
        topEmotion = emotions[results.indices.maxByOrNull { results[it] } ?: 0] // 가장 높은 퍼센티지의 감정 저장
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(this.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(this.contentResolver, uri).copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    private fun getYouTubePlaylistUrl(emotion: String): String {
        return when (emotion) {
            "Angry" -> "https://www.youtube.com/playlist?list=PL60lyX7GL-Xbl_iEIyRB7H8o8P7L9e3B0"
            "Disgust" -> "https://www.youtube.com/playlist?list=PL60lyX7GL-XZ04qluzrPVq5z-H9iG_WVf"
            "Fear" -> "https://www.youtube.com/playlist?list=PL60lyX7GL-Xajt9vMshPPlM3O-kDINHrC"
            "Happy" -> "https://www.youtube.com/playlist?list=PL60lyX7GL-Xa6bRSCL2Efx2qqClJZqLfv"
            "Sad" -> "https://www.youtube.com/playlist?list=YOUR_SAD_PLAYLIST_ID"
            "Surprise" -> "https://www.youtube.com/playlist?list=PL60lyX7GL-XapuyHl4vnYX591CC5HiGw6"
            "Neutral" -> "https://www.youtube.com/playlist?list=PL60lyX7GL-XY3O1LvoayND9mvG_B9auvx"
            else -> "https://www.youtube.com/"
        }
    }
}