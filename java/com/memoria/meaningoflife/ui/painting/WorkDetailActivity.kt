package com.memoria.meaningoflife.ui.painting

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.memoria.meaningoflife.R
import com.memoria.meaningoflife.databinding.ActivityWorkDetailBinding
import com.memoria.meaningoflife.ui.BaseActivity

class WorkDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityWorkDetailBinding
    private lateinit var viewModel: WorkDetailViewModel
    private lateinit var nodeAdapter: NodeListAdapter

    private var workId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        workId = intent.getLongExtra("work_id", 0)
        if (workId == 0L) {
            finish()
            return
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        supportActionBar?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(primaryColor))

        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
        observeData()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[WorkDetailViewModel::class.java]
        viewModel.loadWork(workId)
    }

    private fun setupRecyclerView() {
        nodeAdapter = NodeListAdapter(
            onNodeClick = { node ->
                // 点击节点，打开编辑页面
                val intent = Intent(this, NodeEditActivity::class.java)
                intent.putExtra("work_id", workId)
                intent.putExtra("node_id", node.id)
                startActivity(intent)
            },
            onNodeDelete = { node ->
                AlertDialog.Builder(this)
                    .setTitle("删除节点")
                    .setMessage("确定要删除这个节点吗？")
                    .setPositiveButton("删除") { _, _ ->
                        viewModel.deleteNode(node)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        )

        binding.recyclerViewNodes.apply {
            layoutManager = LinearLayoutManager(this@WorkDetailActivity)
            adapter = nodeAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddNode.setOnClickListener {
            val intent = Intent(this, NodeEditActivity::class.java)
            intent.putExtra("work_id", workId)
            startActivity(intent)
        }

        binding.btnEditWork.setOnClickListener {
            // 编辑作品信息
            val intent = Intent(this, AddWorkActivity::class.java)
            intent.putExtra("work_id", workId)
            startActivity(intent)
        }

        binding.btnDeleteWork.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("删除作品")
                .setMessage("确定要删除这个作品及其所有节点吗？")
                .setPositiveButton("删除") { _, _ ->
                    viewModel.deleteWork()
                    finish()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun observeData() {
        viewModel.work.observe(this) { work ->
            if (work == null) {
                finish()
                return@observe
            }

            binding.tvTitle.text = work.title
            binding.tvDate.text = work.createdDate
            binding.tvTotalDuration.text = "${work.totalDuration / 60}小时${work.totalDuration % 60}分钟"

            if (!work.finalImagePath.isNullOrEmpty()) {
                Glide.with(this)
                    .load(work.finalImagePath)
                    .into(binding.ivFinalImage)
            }
        }

        viewModel.nodes.observe(this) { nodes ->
            nodeAdapter.submitList(nodes)

            if (nodes.isEmpty()) {
                binding.tvEmptyNodes.visibility = android.view.View.VISIBLE
                binding.recyclerViewNodes.visibility = android.view.View.GONE
            } else {
                binding.tvEmptyNodes.visibility = android.view.View.GONE
                binding.recyclerViewNodes.visibility = android.view.View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}