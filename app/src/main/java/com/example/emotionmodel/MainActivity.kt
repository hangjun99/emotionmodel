package com.example.emotionmodel

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.IOException
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var selectedImage: ImageView
    private lateinit var uploadButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var emotionClassifier: EmotionClassifier
    private lateinit var buttonOpenWeb: Button
    private lateinit var pieChart: PieChart
    private var topEmotion: String? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            displayImage(it)
            classifyEmotion(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        selectedImage = findViewById(R.id.selected_image)
        uploadButton = findViewById(R.id.upload_button)
        resultTextView = findViewById(R.id.result_text_view)
        emotionClassifier = EmotionClassifier(this)
        buttonOpenWeb = findViewById(R.id.button_open_web)
        pieChart = findViewById(R.id.pie_chart)

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
        val grayBitmap = convertToGrayscale(bitmap)
        detectFace(grayBitmap) { faceBitmap ->
            if (faceBitmap != null) {
                val resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, 48, 48, true)
                val convertedBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true)
                val results = emotionClassifier.classifyEmotion(convertedBitmap)

                val emotions = arrayOf("Angry", "Disgust", "Fear", "Happy", "Sad", "Surprise", "Neutral")
                val resultsText = emotions.zip(results.toTypedArray()).joinToString("\n") { "${it.first}: ${"%.2f".format(it.second * 100)}%" }

                resultTextView.text = resultsText
                topEmotion = emotions[results.indices.maxByOrNull { results[it] } ?: 0]

                setPieChart(emotions, results)
            } else {
                resultTextView.text = "얼굴을 검출하지 못했습니다."
            }
        }
    }

    private fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val colorFilter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorFilter
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayBitmap
    }

    private fun detectFace(bitmap: Bitmap, callback: (Bitmap?) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(if (false) FaceDetectorOptions.CONTOUR_MODE_ALL else FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setLandmarkMode(if (false) FaceDetectorOptions.LANDMARK_MODE_ALL else FaceDetectorOptions.LANDMARK_MODE_NONE)
            .build()
        val detector = FaceDetection.getClient(options)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val bounds = face.boundingBox
                    val faceBitmap = Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height())
                    callback(faceBitmap)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FaceDetection", "Face detection failed", e)
                callback(null)
            }
    }

    private fun setPieChart(emotions: Array<String>, results: FloatArray) {
        val entries: ArrayList<PieEntry> = ArrayList()
        for (i in emotions.indices) {
            entries.add(PieEntry(results[i] * 100, emotions[i]))
        }

        val dataSet = PieDataSet(entries, "Emotion Distribution")
        val rainbowColors = intArrayOf(
            0xFFE57373.toInt(),
            0xFFFFB74D.toInt(),
            0xFFFFF176.toInt(),
            0xFF81C784.toInt(),
            0xFF64B5F6.toInt(),
            0xFF9575CD.toInt(),
            0xFFBA68C8.toInt()
        )

        dataSet.colors = rainbowColors.toList()
        val data = PieData(dataSet)

        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.invalidate()
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