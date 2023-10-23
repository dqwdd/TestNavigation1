package net.megastudy.testnavigation1.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fragmentNaviStack")
data class NaviStack (
    @PrimaryKey
    var fragmentStack: String, // 쌓인 Fragment Stack
    var stackNumber: Long = 0 // 쌓인 Fragment Stack 순서
)