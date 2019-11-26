package com.sapuseven.ya2fa.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TokenDao {
    @Query("SELECT * FROM token")
    fun getAll(): List<Token>

    @Insert
    fun insertAll(vararg tokens: Token)

    @Delete
    fun delete(token: Token)
}
