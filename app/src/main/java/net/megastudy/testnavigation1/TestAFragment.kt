package net.megastudy.testnavigation1

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.megastudy.testnavigation1.base.BaseBindingFragment
import net.megastudy.testnavigation1.databinding.FragmentTestABinding
import net.megastudy.testnavigation1.db.entity.NaviStack
import java.sql.SQLException

class TestAFragment : BaseBindingFragment<FragmentTestABinding>(R.layout.fragment_test_a) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvFragmentA.setOnClickListener {
            val database = NaviApplication.instance
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.e("tetest", "${database.getDatabase().NaviStackDao().insertNaviStack(NaviStack("123", 1))}")
                    Log.e("tetest", "${database.getDatabase().NaviStackDao().getFragmentNaviStack(1)} 22")
                } catch (e: SQLException) {
                    Log.e("tetest", "124 error")
                }
            }
        }
    }
}