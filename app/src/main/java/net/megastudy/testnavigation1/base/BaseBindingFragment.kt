package net.megastudy.testnavigation1.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner

abstract class BaseBindingFragment<T : ViewDataBinding>(@LayoutRes private val layoutId: Int) : Fragment() {

    private var _binding: T? = null
    val binding get() = _binding!!

    lateinit var mContext: Context
    lateinit var lifecycleOwner: LifecycleOwner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        mContext = requireContext()
        lifecycleOwner = this

        return binding.root
    }

    open fun setupEvents() {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}