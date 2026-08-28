package com.mystore.installments.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mystore.installments.data.entity.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(payment: Payment): Long

    @Query("SELECT * FROM payments WHERE saleId = :saleId ORDER BY date DESC")
    fun getForSale(saleId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getById(id: Long): Payment?
}
