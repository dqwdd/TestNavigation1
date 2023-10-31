package net.megastudy.testnavigation1

import android.os.Bundle
import android.view.View
import androidx.navigation.findNavController
import com.google.android.material.sidesheet.SideSheetBehavior
import net.megastudy.testnavigation1.base.BaseBindingFragment
import net.megastudy.testnavigation1.databinding.FragmentTestBinding

class TestFragment : BaseBindingFragment<FragmentTestBinding>(R.layout.fragment_test) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEvents()
    }

    override fun setupEvents() {
        super.setupEvents()

//        val sideSheetBehavior = SideSheetBehavior.from(binding.layoutSidebar)
        val sideSheetBehavior = SideSheetBehavior.from(binding.layoutSidebar)
//            sideSheetBehavior.state = SideSheetBehavior.s

        sideSheetBehavior.coplanarSiblingView = binding.layoutConstraint
//        sideSheetBehavior.setState(SideSheetBehavior.STATE_EXPANDED)
        binding.btnSideView.setOnClickListener {
            sideSheetBehavior.setState(SideSheetBehavior.STATE_EXPANDED)
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