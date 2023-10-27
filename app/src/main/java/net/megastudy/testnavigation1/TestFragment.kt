package net.megastudy.testnavigation1

import android.os.Bundle
import android.view.View
import androidx.navigation.findNavController
import net.megastudy.testnavigation1.base.BaseBindingFragment
import net.megastudy.testnavigation1.databinding.FragmentTestBinding

class TestFragment : BaseBindingFragment<FragmentTestBinding>(R.layout.fragment_test) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEvents()
    }

    override fun setupEvents() {
        super.setupEvents()

        val sideSheetBehavior = SideSheetBehavior.from(binding.layoutSidebar)
//        com.google.android.material.sidesheet.SideSheetBehavior.from(binding.btnSideView).coplanarSiblingView
//            sideSheetBehavior.state = SideSheetBehavior.s

        sideSheetBehavior.peekWidthMin
        sideSheetBehavior.coplanarSiblingView = binding.layoutConstraint
        binding.btnSideView.setOnClickListener {
            sideSheetBehavior.state = SideSheetBehavior.STATE_HALF_EXPANDED
        }


        binding.btnMoveA.setOnClickListener {
            val navController = requireActivity().findNavController(R.id.flMain)
            navController.navigate(R.id.testAFragment)
        }

        binding.btnMoveB.setOnClickListener {
            val navController = requireActivity().findNavController(R.id.flMain)
            navController.navigate(R.id.testBFragment)
        }

        binding.btnMoveC.setOnClickListener {
            val navController = requireActivity().findNavController(R.id.flMain)
            navController.navigate(R.id.testCFragment)
        }
    }
}