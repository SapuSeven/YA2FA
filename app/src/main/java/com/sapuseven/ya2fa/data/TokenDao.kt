package com.sapuseven.ya2fa.data

import androidx.room.*

@Dao
interface TokenDao {
    @Query("SELECT * FROM token")
    fun getAll(): List<Token>

    @Insert
    fun insertAll(vararg tokens: Token)

    @Delete
    fun delete(token: Token)

    @Update
    fun update(token: Token)
}
