package com.memoria.meaningoflife.ui.painting

import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.memoria.meaningoflife.databinding.ActivityAddWorkBinding
import com.memoria.meaningoflife.data.database.painting.WorkEntity
import com.memoria.meaningoflife.data.repository.PaintingRepository
import com.memoria.meaningoflife.ui.BaseActivity
import com.memoria.meaningoflife.utils.DateUtils
import com.memoria.meaningoflife.utils.ImageUtils
import kotlinx.coroutines.launch

class AddWorkActivity : BaseActivity() {

    private lateinit var binding: ActivityAddWorkBinding
    private var selectedImageUri: Uri? = null
    private var savedImagePath: String? = null
    private var workId: Long = 0
    private var existingWork: WorkEntity? = null

    private val repository = PaintingRepository(com.memoria.meaningoflife.MeaningOfLifeApp.instance.database)

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = uri
            binding.ivPreview.setImageURI(it)
            binding.btnSelectImage.text = "更换图片"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddWorkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        workId = intent.getLongExtra("work_id", 0)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = if (workId == 0L) "添加作品" else "编辑作品"

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        supportActionBar?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(primaryColor))

        setupClickListeners()

        if (workId != 0L) {
            loadWork()
        }
    }

    private fun loadWork() {
        lifecycleScope.launch {
            existingWork = repository.getWorkById(workId)
            existingWork?.let { work ->
                binding.etTitle.setText(work.title)
                binding.etDescription.setText(work.description ?: "")
                binding.etDuration.setText(work.totalDuration.toString())
                savedImagePath = work.finalImagePath
                if (!savedImagePath.isNullOrEmpty()) {
                    com.bumptech.glide.Glide
                        .with(this@AddWorkActivity)
                        .load(savedImagePath)
                        .into(binding.ivPreview)
                    binding.btnSelectImage.text = "更换图片"
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            saveWork()
        }
    }

    private fun saveWork() {
        val title = binding.etTitle.text.toString().trim()
        if (title.isEmpty()) {
            binding.etTitle.error = "请输入作品名称"
            return
        }

        val description = binding.etDescription.text.toString().trim()

        val durationStr = binding.etDuration.text.toString().trim()
        val duration = if (durationStr.isNotEmpty()) {
            durationStr.toIntOrNull() ?: 0
        } else {
            0
        }

        // 保存图片
        if (selectedImageUri != null) {
            val bitmap = ImageUtils.getBitmapFromUri(this, selectedImageUri!!)
            if (bitmap != null) {
                val fileName = "work_${System.currentTimeMillis()}.jpg"
                savedImagePath = ImageUtils.saveImageToStorage(this, bitmap, "paintings", fileName)
            }
        }

        val work = WorkEntity(
            id = if (workId != 0L) workId else 0,
            title = title,
            description = description.ifEmpty { null },
            finalImagePath = savedImagePath,
            totalDuration = duration,
            tags = null,
            createdDate = existingWork?.createdDate ?: DateUtils.getCurrentDate(),
            createdTime = existingWork?.createdTime ?: System.currentTimeMillis(),
            updatedTime = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            if (workId == 0L) {
                val id = repository.insertWork(work)
                if (id > 0) {
                    Toast.makeText(this@AddWorkActivity, "保存成功", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddWorkActivity, "保存失败", Toast.LENGTH_SHORT).show()
                }
            } else {
                repository.updateWork(work)
                Toast.makeText(this@AddWorkActivity, "更新成功", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}