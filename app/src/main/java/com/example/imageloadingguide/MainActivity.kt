package com.example.imageloadingguide

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.imageloadingguide.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.log

class MainActivity: AppCompatActivity() {

    var binding: ActivityMainBinding? = null

    val photoPicker = ActivityResultContracts.PickVisualMedia()

    val getImageUri = registerForActivityResult(photoPicker) {
        lifecycleScope.launch(Dispatchers.Default) {
            it ?: return@launch
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val inputStream = contentResolver.openInputStream(it)
//            val path = saveUriContentToFile(it, "saved_photo")
//            path ?: return@launch
//            withContext(Dispatchers.Main) {
//                binding?.progressBar?.visibility = View.VISIBLE
//            }
            BitmapFactory.decodeStream(inputStream, null, options)
            Log.d("samtest", "Image height: ${options.outHeight}")
            Log.d("samtest", "Image width: ${options.outWidth}")
            Log.d("SAMTEST", "Screen width: ${resources.displayMetrics.widthPixels}")
            Log.d("SAMTEST", "Screen height: ${resources.displayMetrics.heightPixels}")
            val actualHeight = options.outHeight
            val actualWidth = options.outWidth
            val ratio = actualWidth.toFloat() / actualHeight.toFloat()
            val requestedWidth = ((resources.displayMetrics.widthPixels / 480) * 146)
            Log.d("SAMTEST", "pixel density: ${resources.displayMetrics.densityDpi}");
            val requestedHeight = (requestedWidth / ratio).toInt()
            Log.d("SAMTEST", "requestedWidth: ${requestedWidth}");
            Log.d("SAMTEST", "requestHeight: ${requestedHeight}");
            val sampleSize =
                calculateInSampleSize(actualHeight, actualWidth, requestedWidth, requestedHeight)
            Log.d("SAMTEST", "sampleSize: ${sampleSize}");
            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize
            val bitmap = BitmapFactory.decodeFile(path, options)
            withContext(Dispatchers.Main) {
                binding?.imageView?.setImageBitmap(bitmap)
//                binding?.progressBar?.visibility = View.GONE
            }
            val e = 101
            bitmap

            withContext(Dispatchers.Main) {
                binding?.imageView?.setImageURI(it)
            }
        }
    }

    private fun calculateInSampleSize (
        actualHeight: Int,
        actualWidth: Int,
        requestedHeight: Int,
        requestedWidth: Int,
    ): Int {
        var inSampleSize = 1

        if (actualHeight > requestedHeight || actualWidth > requestedWidth) {
            /*
                Calculate the largest inSampleSize value that is a power of 2 and keeps both
                height and width larger than the requested height and width.

                Will run ~30 times for 10^9 (way larger than practical use case)

                A power of two value is calculated because the decoder uses a final value by rounding
                down to the nearest power of two, as per the inSampleSize documentation
             */
            while (actualWidth / inSampleSize >= requestedWidth) {
                inSampleSize *= 2
            }
            if(Math.abs(actualWidth/inSampleSize - requestedWidth) > Math.abs(actualWidth/(inSampleSize/2) - requestedWidth)) {
                inSampleSize/=2
            }
        }
        Log.d("SAMTEST", "returning width: ${actualWidth/inSampleSize}");
        Log.d("SAMTEST", "returning height: ${actualHeight/inSampleSize}");
        return inSampleSize
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        setObservers()

        return super.onCreateView(name, context, attrs)
    }

    private fun setObservers() {
        binding?.buttonView?.setOnClickListener {
            getImageUri.launch(PickVisualMediaRequest())
        }
    }

    private fun saveUriContentToFile(uri: Uri, fileName: String): String? {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Log.e("MainActivity", "Failed to open input stream from URI")
            return null
        }

        val file = File(filesDir, fileName)

        try {
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
            runOnUiThread {
                Toast.makeText(this, "Image saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
            return file.absolutePath
        } catch (e: IOException) {
            Log.e("MainActivity", "Error saving file", e)
        } finally {
            inputStream.close()
        }
        return null
    }
}