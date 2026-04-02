package com.memoria.meaningoflife.ui.lunch

import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ActivityAddDishBinding
import com.memoria.meaningoflife.data.database.lunch.DishEntity
import com.memoria.meaningoflife.data.repository.LunchRepository
import com.memoria.meaningoflife.model.SpicyLevel
import com.memoria.meaningoflife.ui.BaseActivity
import kotlinx.coroutines.launch

class AddDishActivity : BaseActivity() {

    private lateinit var binding: ActivityAddDishBinding
    private var selectedSpicyLevel: SpicyLevel = SpicyLevel.NONE
    private var dishId: Long = 0
    private var existingDish: DishEntity? = null

    private val repository = LunchRepository(com.memoria.meaningoflife.MeaningOfLifeApp.instance.database)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dishId = intent.getLongExtra("dish_id", 0)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = if (dishId == 0L) "添加菜品" else "编辑菜品"

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        supportActionBar?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(primaryColor))

        setupSpicySelector()
        setupCuisineSpinner()
        setupClickListeners()

        if (dishId != 0L) {
            loadDish()
        }
    }

    private fun setupSpicySelector() {
        binding.chipGroupSpicy.setOnCheckedChangeListener { _, checkedId ->
            selectedSpicyLevel = when (checkedId) {
                R.id.chipNone -> SpicyLevel.NONE
                R.id.chipMild -> SpicyLevel.MILD
                R.id.chipMedium -> SpicyLevel.MEDIUM
                R.id.chipHot -> SpicyLevel.HOT
                else -> SpicyLevel.NONE
            }
        }
    }

    private fun setupCuisineSpinner() {
        val cuisines = listOf(
            "川菜", "湘菜", "粤菜", "闽菜", "浙菜", "苏菜", "徽菜", "鲁菜",
            "日料", "韩餐", "西餐", "东南亚菜", "快餐", "面食", "其他"
        )

        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cuisines)
        binding.spinnerCuisine.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveDish()
        }
    }

    private fun loadDish() {
        lifecycleScope.launch {
            existingDish = repository.getDishById(dishId)
            existingDish?.let { dish ->
                binding.etName.setText(dish.name)

                val cuisines = listOf(
                    "川菜", "湘菜", "粤菜", "闽菜", "浙菜", "苏菜", "徽菜", "鲁菜",
                    "日料", "韩餐", "西餐", "东南亚菜", "快餐", "面食", "其他"
                )
                val position = cuisines.indexOf(dish.cuisine)
                if (position >= 0) {
                    binding.spinnerCuisine.setSelection(position)
                }

                selectedSpicyLevel = SpicyLevel.fromValue(dish.spicyLevel)
                when (selectedSpicyLevel) {
                    SpicyLevel.NONE -> binding.chipNone.isChecked = true
                    SpicyLevel.MILD -> binding.chipMild.isChecked = true
                    SpicyLevel.MEDIUM -> binding.chipMedium.isChecked = true
                    SpicyLevel.HOT -> binding.chipHot.isChecked = true
                }

                binding.switchActive.isChecked = dish.isActive
            }
        }
    }

    private fun saveDish() {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            binding.etName.error = "请输入菜名"
            return
        }

        val cuisine = binding.spinnerCuisine.selectedItem.toString()

        val dish = DishEntity(
            id = if (dishId != 0L) dishId else 0,
            name = name,
            cuisine = cuisine,
            spicyLevel = selectedSpicyLevel.value,
            isActive = binding.switchActive.isChecked,
            sortOrder = existingDish?.sortOrder ?: 0
        )

        lifecycleScope.launch {
            if (dishId == 0L) {
                repository.insertDish(dish)
                Toast.makeText(this@AddDishActivity, "添加成功", Toast.LENGTH_SHORT).show()
            } else {
                repository.updateDish(dish)
                Toast.makeText(this@AddDishActivity, "更新成功", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}