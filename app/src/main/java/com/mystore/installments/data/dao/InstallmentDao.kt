package com.mystore.installments.data.dao

import androidx.room.*
import com.mystore.installments.data.entity.Installment
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments WHERE saleId = :saleId ORDER BY installmentNumber ASC")
    fun getForSale(saleId: Long): Flow<List<Installment>>

    @Query("SELECT * FROM installments ORDER BY dueDate ASC")
    fun getAll(): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE status != 'PAID' ORDER BY dueDate ASC")
    fun getUnpaid(): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE status != 'PAID' AND dueDate < :now ORDER BY dueDate ASC")
    fun getOverdue(now: Long): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getById(id: Long): Installment?

    @Insert
    suspend fun insertAll(installments: List<Installment>)

    @Update
    suspend fun update(installment: Installment)
}
