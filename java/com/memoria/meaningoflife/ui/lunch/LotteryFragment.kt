package com.memoria.meaningoflife.ui.lunch

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.FragmentLotteryBinding
import com.memoria.meaningoflife.data.database.lunch.DishEntity
import com.memoria.meaningoflife.model.SpicyLevel
import kotlin.random.Random

/**
 * 午餐抽选Fragment
 * 可作为独立页面嵌入，也可在LunchActivity中使用
 */
class LotteryFragment : Fragment() {

    private var _binding: FragmentLotteryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LunchViewModel
    private var isRolling = false
    private val rollHandler = Handler(Looper.getMainLooper())
    private var rollRunnable: Runnable? = null

    companion object {
        fun newInstance(): LotteryFragment {
            return LotteryFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentLotteryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[LunchViewModel::class.java]

        setupClickListeners()
        observeData()
        loadTodayRecommendation()
    }

    private fun setupClickListeners() {
        binding.btnLottery.setOnClickListener {
            startLottery()
        }

        binding.btnQuickAdd.setOnClickListener {
            showQuickAddDialog()
        }
    }

    private fun observeData() {
        viewModel.activeDishes.observe(viewLifecycleOwner) { dishes ->
            if (dishes.isEmpty()) {
                binding.tvResult.text = "暂无菜品"
                binding.tvDetail.text = "点击 + 添加菜品"
                binding.btnLottery.isEnabled = false
                binding.tvEmptyHint.visibility = View.VISIBLE
                binding.resultContainer.alpha = 0.6f
            } else {
                binding.btnLottery.isEnabled = true
                binding.tvEmptyHint.visibility = View.GONE
                binding.resultContainer.alpha = 1f
            }
        }
    }

    private fun loadTodayRecommendation() {
        viewModel.getTodayLottery { dish ->
            dish?.let {
                binding.tvResult.text = it.name
                val spicy = SpicyLevel.fromValue(it.spicyLevel)
                binding.tvDetail.text = "${it.cuisine} · ${spicy.icon} ${spicy.text}"
                binding.tvHint.text = "今日推荐"
                binding.tvHint.visibility = View.VISIBLE
            } ?: run {
                binding.tvResult.text = "待抽选"
                binding.tvDetail.text = "点击开始抽选"
                binding.tvHint.visibility = View.GONE
            }
        }
    }

    private fun startLottery() {
        if (isRolling) return

        val dishes = viewModel.activeDishes.value
        if (dishes.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "请先添加菜品", Toast.LENGTH_SHORT).show()
            return
        }

        isRolling = true
        binding.btnLottery.isEnabled = false
        binding.btnLottery.text = "🎲 抽选中..."
        binding.tvHint.visibility = View.GONE

        val startTime = System.currentTimeMillis()
        val rollDuration = 1500L
        var interval = 30L

        rollRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime

                // 随机显示一个菜品
                val randomIndex = Random.nextInt(dishes.size)
                val tempDish = dishes[randomIndex]
                binding.tvResult.text = tempDish.name
                val spicy = SpicyLevel.fromValue(tempDish.spicyLevel)
                binding.tvDetail.text = "${tempDish.cuisine} · ${spicy.icon} ${spicy.text}"

                // 添加滚动动画效果（轻微缩放）
                binding.resultContainer.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(50)
                    .withEndAction {
                        binding.resultContainer.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(50)
                            .start()
                    }
                    .start()

                if (elapsed < rollDuration) {
                    // 动态调整间隔：越接近结束越慢
                    val progress = elapsed.toFloat() / rollDuration
                    interval = (30 + (170 * progress)).toLong()
                    rollHandler.postDelayed(this, interval)
                } else {
                    // 抽选结束，选最终结果
                    val finalDish = dishes.random()
                    binding.tvResult.text = finalDish.name
                    val finalSpicy = SpicyLevel.fromValue(finalDish.spicyLevel)
                    binding.tvDetail.text = "${finalDish.cuisine} · ${finalSpicy.icon} ${finalSpicy.text}"

                    // 震动效果
                    try {
                        val vibrator = requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                        if (vibrator.hasVibrator()) {
                            vibrator.vibrate(50)
                        }
                    } catch (e: Exception) {
                        // 忽略
                    }

                    // 保存抽选结果
                    viewModel.saveLotteryResult(finalDish)

                    // 恢复按钮状态
                    isRolling = false
                    binding.btnLottery.isEnabled = true
                    binding.btnLottery.text = getString(R.string.lunch_start)
                    binding.tvHint.text = "今日推荐"
                    binding.tvHint.visibility = View.VISIBLE

                    Toast.makeText(requireContext(), "今日推荐: ${finalDish.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        rollHandler.post(rollRunnable!!)
    }

    private fun showQuickAddDialog() {
        val dialogBinding = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_quick_add_dish, null)

        val etName = dialogBinding.findViewById<android.widget.EditText>(R.id.etName)
        val spinnerCuisine = dialogBinding.findViewById<android.widget.Spinner>(R.id.spinnerCuisine)
        val chipGroupSpicy = dialogBinding.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupSpicy)

        // 设置菜系选项
        val cuisines = listOf(
            "川菜", "湘菜", "粤菜", "闽菜", "浙菜", "苏菜", "徽菜", "鲁菜",
            "日料", "韩餐", "西餐", "东南亚菜", "快餐", "面食", "其他"
        )
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, cuisines)
        spinnerCuisine.adapter = adapter

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("快速添加菜品")
            .setView(dialogBinding)
            .setPositiveButton("添加") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入菜名", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val cuisine = spinnerCuisine.selectedItem.toString()

                var spicyLevel = 0
                when (chipGroupSpicy.checkedChipId) {
                    R.id.chip_mild -> spicyLevel = 1
                    R.id.chip_medium -> spicyLevel = 2
                    R.id.chip_hot -> spicyLevel = 3
                    else -> spicyLevel = 0
                }

                val dish = DishEntity(
                    name = name,
                    cuisine = cuisine,
                    spicyLevel = spicyLevel,
                    isActive = true
                )

                // 通过ViewModel添加
                androidx.lifecycle.ViewModelProvider(requireActivity())[LunchViewModel::class.java]
                    .addDish(dish) { success ->
                        if (success) {
                            Toast.makeText(requireContext(), "添加成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rollHandler.removeCallbacksAndMessages(null)
        _binding = null
    }
}