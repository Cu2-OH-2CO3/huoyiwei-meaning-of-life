package com.memoria.meaningoflife.ui.lunch

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ActivityLunchBinding
import com.memoria.meaningoflife.data.repository.LunchRepository
import com.memoria.meaningoflife.model.SpicyLevel
import com.memoria.meaningoflife.ui.BaseActivity
import com.memoria.meaningoflife.utils.LogManager
import kotlinx.coroutines.launch
import kotlin.random.Random

class LunchActivity : BaseActivity() {

    private lateinit var binding: ActivityLunchBinding
    private lateinit var repository: LunchRepository
    private var isRolling = false
    private var rollHandler = Handler(Looper.getMainLooper())
    private var rollRunnable: Runnable? = null
    private var activeDishes = listOf<com.memoria.meaningoflife.data.database.lunch.DishEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLunchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.lunch_title)

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        android.util.Log.d("LunchActivity", "Primary color: ${Integer.toHexString(primaryColor)}")

        supportActionBar?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(primaryColor))

        setupButtonStyles(primaryColor)

        repository = LunchRepository(com.memoria.meaningoflife.MeaningOfLifeApp.instance.database)

        setupClickListeners()
        loadActiveDishes()
        loadTodayRecommendation()
    }

    private fun setupButtonStyles(primaryColor: Int) {
        // 设置抽选按钮背景
        val solidDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(primaryColor)
            cornerRadius = 12f
        }
        binding.btnLottery.background = solidDrawable

        // 设置边框按钮背景
        val outlineDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setStroke(2, primaryColor)
            cornerRadius = 12f
            setColor(Color.TRANSPARENT)
        }

        binding.btnEditMenu.background = outlineDrawable
        binding.btnHistory.background = outlineDrawable
        binding.btnEditMenu.setTextColor(primaryColor)
        binding.btnHistory.setTextColor(primaryColor)
    }

    private fun setupClickListeners() {
        binding.btnLottery.setOnClickListener {
            startLottery()
        }

        binding.btnEditMenu.setOnClickListener {
            startActivity(Intent(this, DishManageActivity::class.java))
        }

        binding.btnHistory.setOnClickListener {
            showHistoryDialog()
        }
    }

    private fun loadActiveDishes() {
        lifecycleScope.launch {
            activeDishes = repository.getActiveDishes()
            if (activeDishes.isEmpty()) {
                binding.tvResult.text = "暂无菜品"
                binding.tvDetail.text = "请先添加菜品"
                binding.btnLottery.isEnabled = false
                binding.btnLottery.alpha = 0.5f
            } else {
                binding.btnLottery.isEnabled = true
                binding.btnLottery.alpha = 1f
            }
        }
    }

    private fun loadTodayRecommendation() {
        lifecycleScope.launch {
            val history = repository.getTodayLottery()
            if (history != null) {
                val dish = repository.getDishById(history.dishId)
                dish?.let {
                    binding.tvResult.text = it.name
                    binding.tvDetail.text = "${it.cuisine} · ${SpicyLevel.fromValue(it.spicyLevel).icon} ${SpicyLevel.fromValue(it.spicyLevel).text}"
                }
            }
        }
    }

    private fun startLottery() {
        LogManager.i("PaintingActivity", "PaintingActivity onCreate")
        if (isRolling) return

        if (activeDishes.isEmpty()) {
            Toast.makeText(this, "请先添加菜品", Toast.LENGTH_SHORT).show()
            return
        }

        isRolling = true
        binding.btnLottery.isEnabled = false
        binding.btnLottery.text = "抽选中..."
        binding.btnLottery.alpha = 0.7f

        val startTime = System.currentTimeMillis()
        val rollDuration = 1500L
        var interval = 30L

        rollRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime

                val randomIndex = Random.nextInt(activeDishes.size)
                val tempDish = activeDishes[randomIndex]
                binding.tvResult.text = tempDish.name
                binding.tvDetail.text = "${tempDish.cuisine} · ${SpicyLevel.fromValue(tempDish.spicyLevel).icon} ${SpicyLevel.fromValue(tempDish.spicyLevel).text}"

                if (elapsed < rollDuration) {
                    val progress = elapsed.toFloat() / rollDuration
                    interval = (30 + (170 * progress)).toLong()
                    rollHandler.postDelayed(this, interval)
                } else {
                    val finalDish = activeDishes.random()
                    binding.tvResult.text = finalDish.name
                    binding.tvDetail.text = "${finalDish.cuisine} · ${SpicyLevel.fromValue(finalDish.spicyLevel).icon} ${SpicyLevel.fromValue(finalDish.spicyLevel).text}"

                    try {
                        val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                        if (vibrator.hasVibrator()) {
                            vibrator.vibrate(50)
                        }
                    } catch (e: Exception) { }

                    lifecycleScope.launch {
                        repository.recordLottery(finalDish)
                    }

                    isRolling = false
                    binding.btnLottery.isEnabled = true
                    binding.btnLottery.text = getString(R.string.lunch_start)
                    binding.btnLottery.alpha = 1f

                    Toast.makeText(this@LunchActivity, "今日推荐: ${finalDish.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        rollHandler.post(rollRunnable!!)
    }

    private fun showHistoryDialog() {
        lifecycleScope.launch {
            val history = repository.getRecentHistory()
            if (history.isEmpty()) {
                Toast.makeText(this@LunchActivity, "暂无抽选历史", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val items = history.map { "${it.selectedDate} - ${it.dishName}" }.toTypedArray()
            androidx.appcompat.app.AlertDialog.Builder(this@LunchActivity)
                .setTitle("抽选历史")
                .setItems(items, null)
                .setPositiveButton("关闭", null)
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        rollHandler.removeCallbacksAndMessages(null)
    }
}