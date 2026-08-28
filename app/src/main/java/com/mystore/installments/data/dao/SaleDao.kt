package com.mystore.installments.data.dao

import androidx.room.*
import com.mystore.installments.data.entity.Sale
import com.mystore.installments.data.entity.SaleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAll(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY date DESC")
    fun getByCustomer(customerId: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getById(id: Long): Sale?

    @Insert
    suspend fun insert(sale: Sale): Long

    @Update
    suspend fun update(sale: Sale)

    @Delete
    suspend fun delete(sale: Sale)

    @Insert
    suspend fun insertItems(items: List<SaleItem>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getItemsForSaleFlow(saleId: Long): Flow<List<SaleItem>>
}
