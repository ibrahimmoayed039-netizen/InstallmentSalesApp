package com.mystore.installments.data.dao

import androidx.room.*
import com.mystore.installments.data.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Query(
        "SELECT * FROM products WHERE name LIKE '%' || :query || '%' " +
            "OR barcode LIKE '%' || :query || '%' " +
            "OR category LIKE '%' || :query || '%'"
    )
    fun search(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): Product?

    @Insert
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)
}
