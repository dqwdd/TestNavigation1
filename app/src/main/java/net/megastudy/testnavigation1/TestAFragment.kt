package net.megastudy.testnavigation1

import android.os.Bundle
import android.util.Log
import android.view.View
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

        val database = NaviApplication.instance

        CoroutineScope(Dispatchers.IO).launch {
            Log.e("tetest", "0101 ${database.getDatabase().naviStackDao().getAll()}")
        }

        binding.tvFragmentA.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.e("tetest", "${database.getDatabase().naviStackDao().insertNaviStack(NaviStack("123", 1))}")
                    Log.e("tetest", "${database.getDatabase().naviStackDao().insertNaviStack(NaviStack("222", 2))}")

                    database.getDatabase().naviStackDao().insertNaviStack(NaviStack("332", 3))

                    Log.e("tetest", "${database.getDatabase().naviStackDao().getFragmentNaviStack(1)}")
                    Log.e("tetest", "${database.getDatabase().naviStackDao().getFragmentNaviStack(2)}")
                    Log.e("tetest", "${database.getDatabase().naviStackDao().getFragmentNaviStack(3)}")
                } catch (e: SQLException) {
                    Log.e("tetest", "124 tvFragmentA error")
                }
            }
        }

        binding.tvFragmentDB.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    database.getDatabase().naviStackDao().deleteStackFromFragmentStack("222")
                    Log.e("tetest", "01001 ${database.getDatabase().naviStackDao().getAll()}")
                } catch (e: SQLException) {
                    Log.e("tetest", "124 tvFragmentDB error")
                }
            }
        }
    }
}