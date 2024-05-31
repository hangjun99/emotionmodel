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
        val output = Array(1) { FloatArray(7) } // TensorFlow Lite 모델의 출력 형태에 맞게 배열 선언
        interpreter.run(inputBuffer, output) // 출력을 바로 저장할 배열을 전달
        return output[0] // 배열의 첫 번째 요소를 반환
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val width = 48
        val height = 48
        val byteBuffer = ByteBuffer.allocateDirect(4 * width * height * 3) // 4 bytes per float, 3 channels
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(width * height)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
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