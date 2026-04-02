package com.memoria.meaningoflife.ui.diary

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ActivityDiaryDetailBinding
import com.memoria.meaningoflife.model.Mood
import com.memoria.meaningoflife.model.Weather
import com.memoria.meaningoflife.ui.BaseActivity
import com.memoria.meaningoflife.utils.DateUtils
import com.memoria.meaningoflife.utils.JsonHelper

class DiaryDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityDiaryDetailBinding
    private lateinit var viewModel: DiaryDetailViewModel
    private var diaryId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiaryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        diaryId = intent.getLongExtra("diary_id", 0)
        if (diaryId == 0L) {
            finish()
            return
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        supportActionBar?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(primaryColor))

        setupViewModel()
        observeData()
        setupClickListeners()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[DiaryDetailViewModel::class.java]
        viewModel.loadDiary(diaryId)
    }

    private fun observeData() {
        viewModel.diary.observe(this) { diary ->
            if (diary == null) {
                finish()
                return@observe
            }

            // 修复：处理可能为 null 的 mood 和 weather
            val moodValue = diary.mood ?: 2  // 默认值为 2（平静）
            val weatherValue = diary.weather ?: 0  // 默认值为 0（晴天）

            val mood = Mood.fromValue(moodValue)
            val weather = Weather.fromValue(weatherValue)

            binding.tvDate.text = DateUtils.formatDate(diary.createdDate)
            binding.tvMood.text = "${mood.icon} ${mood.text}"
            binding.tvWeather.text = "${weather.icon} ${weather.text}"

            if (!diary.title.isNullOrEmpty()) {
                binding.tvTitle.text = diary.title
                binding.tvTitle.visibility = android.view.View.VISIBLE
            } else {
                binding.tvTitle.visibility = android.view.View.GONE
            }

            binding.tvContent.text = diary.content

            val tags = diary.tags?.let { JsonHelper.jsonToTags(it) } ?: emptyList()
            if (tags.isNotEmpty()) {
                binding.tvTags.text = tags.joinToString(" · ")
                binding.tvTags.visibility = android.view.View.VISIBLE
            } else {
                binding.tvTags.visibility = android.view.View.GONE
            }

            val images = diary.images?.let { JsonHelper.jsonToImages(it) } ?: emptyList()
            if (images.isNotEmpty()) {
                binding.imageContainer.visibility = android.view.View.VISIBLE
                binding.imageContainer.removeAllViews()

                images.forEach { imagePath ->
                    val imageView = android.widget.ImageView(this@DiaryDetailActivity)
                    imageView.layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        400
                    ).apply {
                        bottomMargin = 8
                    }
                    imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    Glide.with(this@DiaryDetailActivity)
                        .load(imagePath)
                        .into(imageView)
                    binding.imageContainer.addView(imageView)
                }
            } else {
                binding.imageContainer.visibility = android.view.View.GONE
            }

            binding.tvUpdateTime.text = "最后编辑: ${DateUtils.formatTime(diary.updatedTime)}"
        }
    }

    private fun setupClickListeners() {
        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, DiaryWriteActivity::class.java)
            intent.putExtra("diary_id", diaryId)
            startActivity(intent)
            finish()
        }

        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("删除日记")
                .setMessage("确定要删除这篇日记吗？")
                .setPositiveButton("删除") { _, _ ->
                    viewModel.deleteDiary()
                    Toast.makeText(this, "日记已删除", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}