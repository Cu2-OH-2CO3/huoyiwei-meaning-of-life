package com.memoria.meaningoflife.ui.diary

import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ActivityDiaryWriteBinding
import com.memoria.meaningoflife.data.database.diary.DiaryEntity
import com.memoria.meaningoflife.data.repository.DiaryRepository
import com.memoria.meaningoflife.model.Mood
import com.memoria.meaningoflife.model.Weather
import com.memoria.meaningoflife.ui.BaseActivity
import com.memoria.meaningoflife.utils.DateUtils
import com.memoria.meaningoflife.utils.ImageUtils
import com.memoria.meaningoflife.utils.JsonHelper
import com.memoria.meaningoflife.utils.LogManager
import kotlinx.coroutines.launch

class DiaryWriteActivity : BaseActivity() {

    private lateinit var binding: ActivityDiaryWriteBinding
    private var selectedMood: Mood = Mood.NORMAL
    private var selectedWeather: Weather = Weather.SUNNY
    private val selectedImages = mutableListOf<String>()
    private val selectedTags = mutableListOf<String>()

    private val repository = DiaryRepository(com.memoria.meaningoflife.MeaningOfLifeApp.instance.database)
    private var existingDiary: DiaryEntity? = null
    private var diaryId: Long = 0

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            saveAndAddImage(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiaryWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        diaryId = intent.getLongExtra("diary_id", 0)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = if (diaryId == 0L) "写日记" else "编辑日记"

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        supportActionBar?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(primaryColor))

        setupMoodSelector()
        setupWeatherSelector()
        setupTagSelector()
        setupClickListeners()

        if (diaryId != 0L) {
            loadDiary()
        }
    }

    private fun setupMoodSelector() {
        val moods = listOf(
            Mood.TERRIBLE, Mood.BAD, Mood.NORMAL, Mood.GOOD, Mood.EXCITED
        )

        binding.chipGroupMood.removeAllViews()
        moods.forEach { mood ->
            val chip = com.google.android.material.chip.Chip(this)
            chip.text = "${mood.icon} ${mood.text}"
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedMood = mood
                }
            }
            binding.chipGroupMood.addView(chip)
        }
        (binding.chipGroupMood.getChildAt(2) as com.google.android.material.chip.Chip).isChecked = true
    }

    private fun setupWeatherSelector() {
        val weathers = listOf(
            Weather.SUNNY, Weather.PARTLY_CLOUDY, Weather.CLOUDY,
            Weather.RAINY, Weather.SNOWY, Weather.FOGGY
        )

        binding.chipGroupWeather.removeAllViews()
        weathers.forEach { weather ->
            val chip = com.google.android.material.chip.Chip(this)
            chip.text = "${weather.icon} ${weather.text}"
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedWeather = weather
                }
            }
            binding.chipGroupWeather.addView(chip)
        }
        (binding.chipGroupWeather.getChildAt(0) as com.google.android.material.chip.Chip).isChecked = true
    }

    private fun setupTagSelector() {
        val presetTags = listOf("日常", "灵感", "复盘", "旅行", "美食", "心情")

        binding.chipGroupTags.removeAllViews()
        presetTags.forEach { tag ->
            val chip = com.google.android.material.chip.Chip(this)
            chip.text = tag
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (!selectedTags.contains(tag)) {
                        selectedTags.add(tag)
                    }
                } else {
                    selectedTags.remove(tag)
                }
            }
            binding.chipGroupTags.addView(chip)
        }

        val addChip = com.google.android.material.chip.Chip(this)
        addChip.text = "+ 自定义"
        addChip.isCheckable = false
        addChip.setOnClickListener {
            showAddTagDialog()
        }
        binding.chipGroupTags.addView(addChip)
    }

    private fun setupClickListeners() {
        binding.btnAddImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            saveDiary()
        }
    }

    private fun showAddTagDialog() {
        val input = android.widget.EditText(this)
        input.hint = "输入新标签"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("添加自定义标签")
            .setView(input)
            .setPositiveButton("添加") { _, _ ->
                val newTag = input.text.toString().trim()
                if (newTag.isNotEmpty() && !selectedTags.contains(newTag)) {
                    selectedTags.add(newTag)
                    val chip = com.google.android.material.chip.Chip(this)
                    chip.text = newTag
                    chip.isCheckable = true
                    chip.isChecked = true
                    chip.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            if (!selectedTags.contains(newTag)) selectedTags.add(newTag)
                        } else {
                            selectedTags.remove(newTag)
                        }
                    }
                    binding.chipGroupTags.addView(chip, binding.chipGroupTags.childCount - 1)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveAndAddImage(uri: Uri) {
        val bitmap = ImageUtils.getBitmapFromUri(this, uri)
        if (bitmap != null) {
            val fileName = "diary_${System.currentTimeMillis()}_${selectedImages.size}.jpg"
            val path = ImageUtils.saveImageToStorage(this, bitmap, "diaries/images", fileName)
            if (path != null) {
                selectedImages.add(path)
                updateImagePreview()
            }
        }
    }

    private fun updateImagePreview() {
        binding.imagePreviewContainer.removeAllViews()

        if (selectedImages.isEmpty()) {
            binding.imagePreviewContainer.visibility = View.GONE
            return
        }

        binding.imagePreviewContainer.visibility = View.VISIBLE
        binding.imagePreviewContainer.layoutManager = GridLayoutManager(this, 3)

        val adapter = ImagePreviewAdapter(selectedImages) { position ->
            selectedImages.removeAt(position)
            updateImagePreview()
        }
        binding.imagePreviewContainer.adapter = adapter
    }

    private fun loadDiary() {
        lifecycleScope.launch {
            existingDiary = repository.getDiaryById(diaryId)
            existingDiary?.let { diary ->
                binding.etTitle.setText(diary.title)
                binding.etContent.setText(diary.content)
                // 修复：处理可能为 null 的 mood 和 weather
                val moodValue = diary.mood ?: 2  // 默认 NORMAL
                val weatherValue = diary.weather ?: 0  // 默认 SUNNY
                selectedMood = Mood.fromValue(moodValue)
                selectedWeather = Weather.fromValue(weatherValue)

                diary.tags?.let { tagsJson ->
                    val tags = JsonHelper.jsonToTags(tagsJson)
                    selectedTags.addAll(tags)
                    for (i in 0 until binding.chipGroupTags.childCount) {
                        val chip = binding.chipGroupTags.getChildAt(i) as? com.google.android.material.chip.Chip
                        if (chip != null && tags.contains(chip.text.toString())) {
                            chip.isChecked = true
                        }
                    }
                }

                diary.images?.let { imagesJson ->
                    selectedImages.addAll(JsonHelper.jsonToImages(imagesJson))
                    updateImagePreview()
                }

                for (i in 0 until binding.chipGroupMood.childCount) {
                    val chip = binding.chipGroupMood.getChildAt(i) as? com.google.android.material.chip.Chip
                    if (chip != null && chip.text.contains(selectedMood.text)) {
                        chip.isChecked = true
                    }
                }

                for (i in 0 until binding.chipGroupWeather.childCount) {
                    val chip = binding.chipGroupWeather.getChildAt(i) as? com.google.android.material.chip.Chip
                    if (chip != null && chip.text.contains(selectedWeather.text)) {
                        chip.isChecked = true
                    }
                }
            }
        }
    }

    private fun saveDiary() {
        LogManager.i("DiaryWriteActivity", "Saving diary")
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()

        if (content.isEmpty()) {
            binding.etContent.error = "请填写日记内容"
            return
        }

        val diary = DiaryEntity(
            id = if (diaryId != 0L) diaryId else 0,
            title = title.ifEmpty { null },
            content = content,
            mood = selectedMood.value,
            weather = selectedWeather.value,
            tags = if (selectedTags.isNotEmpty()) JsonHelper.tagsToJson(selectedTags) else null,
            images = if (selectedImages.isNotEmpty()) JsonHelper.imagesToJson(selectedImages) else null,
            createdDate = existingDiary?.createdDate ?: DateUtils.getCurrentDate(),
            createdTime = existingDiary?.createdTime ?: System.currentTimeMillis(),
            updatedTime = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            if (diaryId == 0L) {
                repository.insertDiary(diary)
                Toast.makeText(this@DiaryWriteActivity, "日记已保存", Toast.LENGTH_SHORT).show()
            } else {
                repository.updateDiary(diary)
                Toast.makeText(this@DiaryWriteActivity, "日记已更新", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class ImagePreviewAdapter(
        private val images: List<String>,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_image_preview, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val imagePath = images[position]
            Glide.with(holder.itemView.context)
                .load(imagePath)
                .centerCrop()
                .into(holder.imageView)

            holder.deleteButton.setOnClickListener {
                onDelete(position)
            }
        }

        override fun getItemCount() = images.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: android.widget.ImageView = itemView.findViewById(R.id.ivImage)
            val deleteButton: android.widget.ImageView = itemView.findViewById(R.id.ivDelete)
        }
    }
}