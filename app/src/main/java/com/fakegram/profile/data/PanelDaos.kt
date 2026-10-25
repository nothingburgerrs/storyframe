package com.fakegram.profile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PanelDao {
    @Query("SELECT * FROM panels ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PanelEntity>>

    @Query("SELECT * FROM panels WHERE id = :id")
    fun observeById(id: Long): Flow<PanelEntity?>

    @Query("SELECT * FROM panels WHERE id = :id")
    suspend fun getById(id: Long): PanelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(panel: PanelEntity): Long

    @Update
    suspend fun update(panel: PanelEntity)

    @Query("DELETE FROM panels WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PanelImageDao {
    @Query("SELECT * FROM panel_images WHERE panelId = :panelId ORDER BY position ASC")
    fun observeForPanel(panelId: Long): Flow<List<PanelImageEntity>>

    @Query("SELECT * FROM panel_images ORDER BY position ASC")
    fun observeAll(): Flow<List<PanelImageEntity>>

    @Query("SELECT * FROM panel_images WHERE panelId = :panelId ORDER BY position ASC")
    suspend fun getForPanel(panelId: Long): List<PanelImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: PanelImageEntity): Long

    @Update
    suspend fun update(image: PanelImageEntity)

    @Query("DELETE FROM panel_images WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PanelFactDao {
    @Query("SELECT * FROM panel_facts WHERE panelId = :panelId ORDER BY position ASC")
    fun observeForPanel(panelId: Long): Flow<List<PanelFactEntity>>

    @Query("SELECT * FROM panel_facts WHERE panelId = :panelId ORDER BY position ASC")
    suspend fun getForPanel(panelId: Long): List<PanelFactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fact: PanelFactEntity): Long

    @Update
    suspend fun update(fact: PanelFactEntity)

    @Query("DELETE FROM panel_facts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PanelItemDao {
    @Query("SELECT * FROM panel_items WHERE panelId = :panelId ORDER BY position ASC")
    fun observeForPanel(panelId: Long): Flow<List<PanelItemEntity>>

    @Query("SELECT * FROM panel_items WHERE panelId = :panelId ORDER BY position ASC")
    suspend fun getForPanel(panelId: Long): List<PanelItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PanelItemEntity): Long

    @Update
    suspend fun update(item: PanelItemEntity)

    @Query("DELETE FROM panel_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
