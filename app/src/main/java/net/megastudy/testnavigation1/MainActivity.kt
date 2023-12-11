package net.megastudy.testnavigation1

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.plusAssign
import kotlinx.coroutines.launch
import net.megastudy.base.BaseBindingActivity
import net.megastudy.testnavigation1.databinding.ActivityMainBinding
import net.megastudy.testnavigation1.util.KeepStateNavigator

class MainActivity : BaseBindingActivity<ActivityMainBinding>(R.layout.activity_main) {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            setNavigation()
        }
    }

//    private fun setNavigation() {
//        // xml 에서 Fragment 가 재생성 되어 불편할 경우, keep_state_fragment 사용
//        val navController = findNavController(R.id.flMain)
//        val navHostFragment = supportFragmentManager.findFragmentById(R.id.flMain)!!
//        val navigator = KeepStateNavigator(this, navHostFragment.childFragmentManager, R.id.flMain)
//        navController.navigatorProvider += navigator
//        navController.setGraph(R.navigation.navigation_main)
//        navController.addOnDestinationChangedListener { controller, destination, arguments ->
//        }
//    }

    private fun setNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.flMain) as NavHostFragment
        navController = navHostFragment.navController
    }
}