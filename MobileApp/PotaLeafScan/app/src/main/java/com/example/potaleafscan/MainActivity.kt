package com.example.potaleafscan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.potaleafscan.databinding.ActivityMainBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import androidx.core.text.HtmlCompat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentImageFile: File? = null

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_PICK = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCapture.setOnClickListener {
            requestCameraPermission()
        }

        binding.btnUpload.setOnClickListener {
            requestStoragePermission()
        }

        binding.btnPredict.setOnClickListener {
            currentImageFile?.let { file -> uploadImage(file) }
        }

        binding.btnClear.setOnClickListener {
            resetUI()
        }

        resetUI()
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_IMAGE_CAPTURE)
        } else {
            openCamera()
        }
    }

    private fun requestStoragePermission() {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_IMAGE_PICK)
        } else {
            openGallery()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val file = File.createTempFile("IMG_", ".jpg", cacheDir).apply {
            currentImageFile = this
        }
        val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    currentImageFile?.let {
                        showImage(it)
                    }
                }
                REQUEST_IMAGE_PICK -> {
                    data?.data?.let { uri ->
                        val file = FileUtils.getFileFromUri(this, uri)
                        currentImageFile = file
                        showImage(file)
                    }
                }
            }
        }
    }

    private fun showImage(file: File) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        binding.imageView.setImageBitmap(bitmap)
        binding.imageBoxLayout.visibility = View.VISIBLE
        binding.actionButtonsLayout.visibility = View.VISIBLE
        binding.resultBoxLayout.visibility = View.GONE
    }

    private fun uploadImage(file: File) {
        binding.resultBoxLayout.visibility = View.GONE

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        RetrofitInstance.api.uploadImage(body).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    val confidenceValue = result?.confidence ?: 0.0
                    val formattedConfidence = String.format("%.2f", confidenceValue * 1)
                    val resultText = "<b>${result?.prediction}</b><br><b>Confidence: $formattedConfidence%</b>"
                    binding.tvResult.text = HtmlCompat.fromHtml(resultText, HtmlCompat.FROM_HTML_MODE_LEGACY)

                    binding.resultBoxLayout.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this@MainActivity, "Prediction failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun resetUI() {
        binding.imageView.setImageDrawable(null)
        binding.imageBoxLayout.visibility = View.GONE
        binding.resultBoxLayout.visibility = View.GONE
        binding.actionButtonsLayout.visibility = View.GONE
        currentImageFile = null
    }
}
