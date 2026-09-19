package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_files ORDER BY dateAddedMillis DESC")
    fun getAllFilesFlow(): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE fileType = :type ORDER BY dateAddedMillis DESC")
    fun getFilesByTypeFlow(type: VaultFileType): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE fileName LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY dateAddedMillis DESC")
    fun searchFilesFlow(query: String): Flow<List<VaultFileEntity>>

    @Query("SELECT SUM(fileSizeBytes) FROM vault_files")
    fun getTotalUsedBytesFlow(): Flow<Long?>

    @Query("SELECT * FROM vault_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Long): VaultFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: VaultFileEntity): Long

    @Update
    suspend fun update(file: VaultFileEntity)

    @Delete
    suspend fun delete(file: VaultFileEntity)

    @Query("DELETE FROM vault_files")
    suspend fun deleteAll()
}
