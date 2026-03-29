package com.memoria.meaningoflife.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.memoria.meaningoflife.databinding.FragmentIntroBinding

class IntroFragment : Fragment() {

    private var _binding: FragmentIntroBinding? = null
    private val binding get() = _binding!!

    private var title: String = ""
    private var description: String = ""
    private var iconRes: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentIntroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            title = it.getString("title", "")
            description = it.getString("description", "")
            iconRes = it.getInt("iconRes", 0)
        }

        binding.tvTitle.text = title
        binding.tvDescription.text = description
        if (iconRes != 0) {
            binding.ivIcon.setImageResource(iconRes)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(title: String, description: String, iconRes: Int): IntroFragment {
            val fragment = IntroFragment()
            val args = Bundle()
            args.putString("title", title)
            args.putString("description", description)
            args.putInt("iconRes", iconRes)
            fragment.arguments = args
            return fragment
        }
    }
}