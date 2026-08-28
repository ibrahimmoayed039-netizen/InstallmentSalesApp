package com.mystore.installments.data.dao

import androidx.room.*
import com.mystore.installments.data.entity.Installment
import com.mystore.installments.data.entity.InstallmentWithCustomer
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

    // نفس الأقساط غير المسددة، لكن مربوطة باسم وهاتف العميل عبر جدول المبيعات،
    // وتُرتَّب أولاً حسب اسم العميل ثم تاريخ الاستحقاق ضمن أقساط نفس العميل.
    @Query(
        """
        SELECT installments.*,
               customers.id AS customerId,
               customers.name AS customerName,
               customers.phone AS customerPhone
        FROM installments
        INNER JOIN sales ON installments.saleId = sales.id
        INNER JOIN customers ON sales.customerId = customers.id
        WHERE installments.status != 'PAID'
        ORDER BY customers.name ASC, installments.dueDate ASC
        """
    )
    fun getUnpaidWithCustomer(): Flow<List<InstallmentWithCustomer>>

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getById(id: Long): Installment?

    @Insert
    suspend fun insertAll(installments: List<Installment>)

    @Update
    suspend fun update(installment: Installment)
}
