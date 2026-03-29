package com.memoria.meaningoflife.ui.lunch

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ActivityDishManageBinding
import com.memoria.meaningoflife.data.database.lunch.DishEntity
import com.memoria.meaningoflife.data.repository.LunchRepository
import com.memoria.meaningoflife.ui.BaseActivity
import kotlinx.coroutines.launch

class DishManageActivity : BaseActivity() {

    private lateinit var binding: ActivityDishManageBinding
    private lateinit var adapter: DishManageAdapter
    private val repository = LunchRepository(com.memoria.meaningoflife.MeaningOfLifeApp.instance.database)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDishManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "菜单管理"

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        supportActionBar?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(primaryColor))

        setupRecyclerView()
        setupClickListeners()
        loadDishes()
    }

    private fun setupRecyclerView() {
        adapter = DishManageAdapter(
            onToggleActive = { dish ->
                lifecycleScope.launch {
                    repository.updateActiveStatus(dish.id, !dish.isActive)
                    loadDishes()
                }
            },
            onEdit = { dish ->
                val intent = Intent(this, AddDishActivity::class.java)
                intent.putExtra("dish_id", dish.id)
                startActivity(intent)
            },
            onDelete = { dish ->
                AlertDialog.Builder(this)
                    .setTitle("删除菜品")
                    .setMessage("确定要删除「${dish.name}」吗？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch {
                            repository.deleteDish(dish)
                            loadDishes()
                            Toast.makeText(this@DishManageActivity, "已删除", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@DishManageActivity)
            adapter = this@DishManageActivity.adapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddDishActivity::class.java))
        }
    }

    private fun loadDishes() {
        lifecycleScope.launch {
            val dishes = repository.getAllDishesSync()
            adapter.submitList(dishes)

            if (dishes.isEmpty()) {
                binding.tvEmpty.visibility = android.view.View.VISIBLE
                binding.recyclerView.visibility = android.view.View.GONE
            } else {
                binding.tvEmpty.visibility = android.view.View.GONE
                binding.recyclerView.visibility = android.view.View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        loadDishes()
    }
}