package com.sapuseven.ya2fa.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Token::class], version = 1)
abstract class TokenDatabase : RoomDatabase() {
    abstract fun tokenDao(): TokenDao
}
