package net.megastudy.testnavigation1

import android.app.Application
import net.megastudy.testnavigation1.db.LocalDatabase

class NaviApplication : Application() {

    init {
        instance = this
    }

    fun getDatabase() = LocalDatabase.getDBInstance(this)

    companion object {
        lateinit var instance: NaviApplication
    }
}