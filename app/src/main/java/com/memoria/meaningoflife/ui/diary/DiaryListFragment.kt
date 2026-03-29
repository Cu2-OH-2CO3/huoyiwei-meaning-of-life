package com.memoria.meaningoflife.ui.diary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.memoria.meaningoflife.databinding.FragmentDiaryListBinding

class DiaryListFragment : Fragment() {

    private var _binding: FragmentDiaryListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DiaryViewModel
    private lateinit var adapter: DiaryListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDiaryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[DiaryViewModel::class.java]

        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = DiaryListAdapter { diary ->
            // 跳转到详情
            val intent = android.content.Intent(requireContext(), DiaryDetailActivity::class.java)
            intent.putExtra("diary_id", diary.id)
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DiaryListFragment.adapter
        }
    }

    private fun observeData() {
        viewModel.allDiaries.observe(viewLifecycleOwner) { diaries ->
            adapter.submitList(diaries)

            if (diaries.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}