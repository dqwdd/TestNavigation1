package net.megastudy.testnavigation1.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.megastudy.testnavigation1.db.entity.NaviStack

@Database(entities = [NaviStack::class], version = 2)
abstract class LocalDatabase : RoomDatabase() {

    abstract fun naviStackDao() : NaviStackDao

    companion object {
        private const val dbName = "testDB"
        private var instance: LocalDatabase? = null

        fun getDBInstance(mContext: Context): LocalDatabase {
            return instance ?: synchronized(this) {
                val mInstance = Room.databaseBuilder(mContext.applicationContext, LocalDatabase::class.java, dbName)
//                    .addCallback(
//                        object : Callback() {
//                            override fun onCreate(db: SupportSQLiteDatabase) {
//                                super.onCreate(db) // 많은 양의 데이터를 초기화 하려면 작업쓰레드에서 이곳에서 처리하면 된다
//                            }
//                        }
//                    )
                    .fallbackToDestructiveMigration()
                    .build()

                mInstance
            }
        }
    }
}