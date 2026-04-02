package com.memoria.meaningoflife.ui.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ActivityBackgroundPreviewBinding
import com.memoria.meaningoflife.ui.BaseActivity
import com.memoria.meaningoflife.utils.BackgroundManager
import java.io.File

class BackgroundPreviewActivity : BaseActivity() {

    private lateinit var binding: ActivityBackgroundPreviewBinding
    private var currentAlpha = 100
    private var currentImageUri: Uri? = null
    private var currentBitmap: Bitmap? = null

    companion object {
        private const val TAG = "BackgroundPreview"
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            Log.d(TAG, "Image picked: $it")
            currentImageUri = it
            loadAndPreviewImage(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackgroundPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "背景预览"

        Log.d(TAG, "onCreate: Activity started")

        BackgroundManager.init(this)

        setupClickListeners()
        setupSeekBar()
        loadCurrentBackground()
    }

    private fun setupClickListeners() {
        binding.btnSelectImage.setOnClickListener {
            Log.d(TAG, "Select image button clicked")
            pickImageLauncher.launch("image/*")
        }

        binding.btnApply.setOnClickListener {
            Log.d(TAG, "Apply button clicked")
            applyBackground()
        }

        binding.btnClear.setOnClickListener {
            Log.d(TAG, "Clear button clicked")
            clearBackground()
        }
    }

    private fun setupSeekBar() {
        binding.seekBarAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentAlpha = progress
                binding.tvAlphaValue.text = "$progress%"
                Log.d(TAG, "Alpha changed: $progress%")
                updatePreviewAlpha(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun loadCurrentBackground() {
        Log.d(TAG, "loadCurrentBackground: Start")

        val isEnabled = BackgroundManager.isBackgroundEnabled()
        Log.d(TAG, "loadCurrentBackground: isEnabled=$isEnabled")

        if (isEnabled) {
            val path = BackgroundManager.getBackgroundPath()
            Log.d(TAG, "loadCurrentBackground: path=$path")

            if (!path.isNullOrEmpty()) {
                val file = File(path)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    if (bitmap != null) {
                        currentBitmap = bitmap
                        displayBitmap(bitmap)
                        Log.d(TAG, "loadCurrentBackground: Loaded existing background, size=${bitmap.width}x${bitmap.height}")
                    } else {
                        Log.e(TAG, "loadCurrentBackground: Failed to decode bitmap")
                    }
                } else {
                    Log.e(TAG, "loadCurrentBackground: File not exists: $path")
                }
            }

            val alpha = BackgroundManager.getBackgroundAlpha()
            currentAlpha = alpha
            binding.seekBarAlpha.progress = alpha
            binding.tvAlphaValue.text = "$alpha%"
            Log.d(TAG, "loadCurrentBackground: alpha=$alpha")
        }
    }

    private fun loadAndPreviewImage(uri: Uri) {
        try {
            Log.d(TAG, "loadAndPreviewImage: uri=$uri")

            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap != null) {
                Log.d(TAG, "loadAndPreviewImage: Original bitmap loaded, size=${originalBitmap.width}x${originalBitmap.height}")

                // 获取设备屏幕尺寸
                val displayMetrics = resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels

                Log.d(TAG, "loadAndPreviewImage: Screen size=${screenWidth}x${screenHeight}")

                // 计算缩放比例，保持图片比例，适应屏幕宽度
                val scale = screenWidth.toFloat() / originalBitmap.width
                val newHeight = (originalBitmap.height * scale).toInt()

                // 如果新高度超过屏幕高度，按高度缩放
                val finalScale = if (newHeight > screenHeight) {
                    screenHeight.toFloat() / originalBitmap.height
                } else {
                    scale
                }

                val targetWidth = (originalBitmap.width * finalScale).toInt()
                val targetHeight = (originalBitmap.height * finalScale).toInt()

                Log.d(TAG, "loadAndPreviewImage: Target size=${targetWidth}x${targetHeight}")

                // 缩放图片
                val matrix = Matrix()
                matrix.postScale(finalScale, finalScale)
                val scaledBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)

                currentBitmap = scaledBitmap
                displayBitmap(scaledBitmap)
            } else {
                Log.e(TAG, "loadAndPreviewImage: Failed to decode bitmap")
                Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadAndPreviewImage: Error", e)
            Toast.makeText(this, "图片加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayBitmap(bitmap: Bitmap) {
        val drawable = android.graphics.drawable.BitmapDrawable(resources, bitmap)
        drawable.alpha = (currentAlpha * 2.55).toInt()
        binding.ivPreview.setImageDrawable(drawable)

        binding.tvHint.visibility = android.view.View.GONE
        binding.tvNoImage.visibility = android.view.View.GONE
        binding.ivPreview.visibility = android.view.View.VISIBLE
        binding.ivPreview.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
    }

    private fun updatePreviewAlpha(alpha: Int) {
        currentBitmap?.let { bitmap ->
            val drawable = android.graphics.drawable.BitmapDrawable(resources, bitmap)
            drawable.alpha = (alpha * 2.55).toInt()
            binding.ivPreview.setImageDrawable(drawable)
            Log.d(TAG, "updatePreviewAlpha: Updated to $alpha%")
        }
    }

    private fun applyBackground() {
        Log.d(TAG, "applyBackground: Start, uri=${currentImageUri}, alpha=$currentAlpha")

        if (currentBitmap == null) {
            Log.w(TAG, "applyBackground: No image loaded")
            Toast.makeText(this, "请先选择图片", Toast.LENGTH_SHORT).show()
            return
        }

        // 保存原始图片（不缩放），保持原图质量
        val success = if (currentImageUri != null) {
            BackgroundManager.saveBackgroundImage(this, currentImageUri!!)
        } else {
            false
        }

        Log.d(TAG, "applyBackground: Save result=$success")

        if (success) {
            BackgroundManager.setBackgroundAlpha(currentAlpha)
            Log.d(TAG, "applyBackground: Alpha saved=$currentAlpha")

            Toast.makeText(this, "背景已保存", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Log.e(TAG, "applyBackground: Failed to save background")
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearBackground() {
        Log.d(TAG, "clearBackground: Clearing background")

        AlertDialog.Builder(this)
            .setTitle("清除背景")
            .setMessage("确定要清除自定义背景吗？")
            .setPositiveButton("清除") { _, _ ->
                BackgroundManager.clearBackground()
                Log.d(TAG, "clearBackground: Background cleared")

                binding.ivPreview.setImageDrawable(null)
                binding.ivPreview.visibility = android.view.View.GONE
                binding.tvHint.visibility = android.view.View.VISIBLE
                binding.tvNoImage.visibility = android.view.View.VISIBLE
                currentBitmap = null
                currentImageUri = null

                Toast.makeText(this, "背景已清除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}