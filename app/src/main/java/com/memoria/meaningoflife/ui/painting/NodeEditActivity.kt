package com.memoria.meaningoflife.ui.painting

import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.memoria.meaningoflife.databinding.ActivityNodeEditBinding
import com.memoria.meaningoflife.data.database.painting.NodeEntity
import com.memoria.meaningoflife.data.repository.PaintingRepository
import com.memoria.meaningoflife.ui.BaseActivity
import com.memoria.meaningoflife.utils.ImageUtils
import kotlinx.coroutines.launch

class NodeEditActivity : BaseActivity() {

    private lateinit var binding: ActivityNodeEditBinding
    private var workId: Long = 0
    private var nodeId: Long = 0
    private var existingNode: NodeEntity? = null
    private var selectedImageUri: Uri? = null
    private var savedImagePath: String? = null
    private var durationMode: Int = 0

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
        binding = ActivityNodeEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        workId = intent.getLongExtra("work_id", 0)
        nodeId = intent.getLongExtra("node_id", 0)

        if (workId == 0L) {
            finish()
            return
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = if (nodeId == 0L) "添加节点" else "编辑节点"

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        supportActionBar?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(primaryColor))

        setupClickListeners()
        setupDurationModeToggle()

        if (nodeId != 0L) {
            loadNode()
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            saveNode()
        }
    }

    private fun setupDurationModeToggle() {
        binding.chipGroup.setOnCheckedChangeListener { _, checkedId ->
            durationMode = when (checkedId) {
                binding.chipSingleDuration.id -> 0
                binding.chipCumulativeDuration.id -> 1
                else -> 0
            }

            if (durationMode == 1) {
                binding.etCumulativeHint.visibility = android.view.View.VISIBLE
                binding.etDuration.hint = "请输入累计时长（分钟）"
            } else {
                binding.etCumulativeHint.visibility = android.view.View.GONE
                binding.etDuration.hint = "请输入时长（分钟）"
            }
        }
    }

    private fun loadNode() {
        lifecycleScope.launch {
            existingNode = repository.getNodeById(nodeId)
            existingNode?.let { node ->
                binding.etNote.setText(node.note)
                binding.etDuration.setText(node.duration.toString())
                savedImagePath = node.imagePath
                if (!savedImagePath.isNullOrEmpty()) {
                    com.bumptech.glide.Glide
                        .with(this@NodeEditActivity)
                        .load(savedImagePath)
                        .into(binding.ivPreview)
                    binding.btnSelectImage.text = "更换图片"
                }
                // 灵感记录
                if (!node.inspiration.isNullOrEmpty()) {
                    binding.etInspiration.setText(node.inspiration)
                }
            }
        }
    }

    private fun saveNode() {
        val note = binding.etNote.text.toString().trim()
        val inspiration = binding.etInspiration.text.toString().trim()
        val durationStr = binding.etDuration.text.toString().trim()

        if (durationStr.isEmpty()) {
            binding.etDuration.error = "请输入时长"
            return
        }

        val inputDuration = durationStr.toIntOrNull()
        if (inputDuration == null) {
            binding.etDuration.error = "请输入有效的数字"
            return
        }

        // 保存节点图片
        if (selectedImageUri != null) {
            val bitmap = ImageUtils.getBitmapFromUri(this, selectedImageUri!!)
            if (bitmap != null) {
                val fileName = "node_${System.currentTimeMillis()}.jpg"
                savedImagePath = ImageUtils.saveImageToStorage(this, bitmap, "nodes", fileName)
            }
        }

        lifecycleScope.launch {
            val work = repository.getWorkById(workId)
            val existingNodes = repository.getNodesByWorkId(workId)
            val maxOrder = repository.getMaxNodeOrder(workId)

            val nodeOrder = if (nodeId == 0L) maxOrder + 1 else existingNode?.nodeOrder ?: 0

            val duration: Int
            val cumulativeDuration: Int

            if (durationMode == 1) {
                cumulativeDuration = inputDuration
                val prevCumulative = if (nodeOrder > 0) {
                    existingNodes.find { it.nodeOrder == nodeOrder - 1 }?.cumulativeDuration ?: 0
                } else 0
                duration = cumulativeDuration - prevCumulative
                if (duration < 0) {
                    Toast.makeText(this@NodeEditActivity, "累计时长不能小于上一节点", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            } else {
                duration = inputDuration
                val prevCumulative = if (nodeOrder > 0) {
                    existingNodes.find { it.nodeOrder == nodeOrder - 1 }?.cumulativeDuration ?: 0
                } else 0
                cumulativeDuration = prevCumulative + duration
            }

            val node = NodeEntity(
                id = if (nodeId != 0L) nodeId else 0,
                workId = workId,
                nodeOrder = nodeOrder,
                imagePath = savedImagePath,
                duration = duration,
                cumulativeDuration = cumulativeDuration,
                note = note,
                referenceImagePath = null,
                inspiration = inspiration.ifEmpty { null }
            )

            if (nodeId == 0L) {
                repository.insertNode(node)
            } else {
                repository.updateNode(node)
            }

            // 更新作品总时长
            val allNodes = repository.getNodesByWorkId(workId)
            val totalDuration = allNodes.sumOf { it.duration }
            work?.let {
                val updatedWork = it.copy(totalDuration = totalDuration)
                repository.updateWork(updatedWork)
            }

            Toast.makeText(this@NodeEditActivity, "保存成功", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}