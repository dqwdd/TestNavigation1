package net.megastudy.testnavigation1.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import net.megastudy.testnavigation1.db.entity.NaviStack

@Dao
interface NaviStackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNaviStack(naviStack: NaviStack)

    @Query("SELECT * FROM fragmentNaviStack WHERE stackNumber = :stackNumber")
    fun getFragmentNaviStack(stackNumber: Long): NaviStack


    @Query("SELECT * FROM fragmentNaviStack")
    fun getAll(): List<NaviStack>

//    @Query("SELECT * FROM fragmentNaviStack WHERE stackNumber = :number")
//    fun getFragmentNaviStackOrderNumber(number: Int): List<NaviStack>

    @Update
    fun updateStack(stack: NaviStack)

    @Delete
    fun deleteStack(stack: NaviStack)

    @Query("DELETE FROM fragmentNaviStack WHERE fragmentStack = :fragmentStack")
    fun deleteStackFromFragmentStack(fragmentStack: String)
}