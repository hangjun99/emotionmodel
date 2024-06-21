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

        uploadButton.setOnClickListener {
            getContent.launch("image/*")
        }

        val buttonOpenWeb: Button = findViewById(R.id.button_open_web)
        buttonOpenWeb.setOnClickListener {
            // 웹 페이지 URL ex) 감정분석 결과가 fear(두려움)로 나올때 추천음악 리스트로 이동
            val url = "https://www.youtube.com/playlist?list=PL60lyX7GL-Xajt9vMshPPlM3O-kDINHrC"
            // Intent 생성
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            // Intent 실행
            startActivity(intent)
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
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(this.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source)
            bitmap.copy(Bitmap.Config.ARGB_8888, true) // ARGB_8888로 복사
        } else {
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            bitmap.copy(Bitmap.Config.ARGB_8888, true) // ARGB_8888로 복사
        }
    }
}