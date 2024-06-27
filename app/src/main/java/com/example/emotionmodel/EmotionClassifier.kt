package com.example.emotionmodel

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EmotionClassifier(context: Context) {
    private var interpreter: Interpreter

    init {
        val assetManager = context.assets
        val model = assetManager.open("emotionmodel.tflite").use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(model.size)
        buffer.order(ByteOrder.nativeOrder())
        buffer.put(model)
        interpreter = Interpreter(buffer)
    }

    fun classifyEmotion(bitmap: Bitmap): FloatArray {
        val inputBuffer = convertBitmapToByteBuffer(bitmap)
        val output = Array(1) { FloatArray(7) } // 모델의 출력 크기에 맞게 배열 설정

        interpreter.run(inputBuffer, output) // 예측 수행
        return output[0] // 예측 결과 반환
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val width = 48 // 모델의 입력 너비
        val height = 48 // 모델의 입력 높이

        // 이미지 크기 조정
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

        val byteBuffer = ByteBuffer.allocateDirect(4 * width * height * 3) // 4 bytes per float, 3 channels
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(width * height)
        resizedBitmap.getPixels(intValues, 0, width, 0, 0, width, height)

        for (pixel in intValues) {
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
        return byteBuffer
    }
}