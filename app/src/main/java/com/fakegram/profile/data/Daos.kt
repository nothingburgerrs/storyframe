package com.fakegram.profile.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun observeById(id: Long): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Long): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PillDao {
    @Query("SELECT * FROM pills WHERE profileId = :profileId ORDER BY position ASC")
    fun observeForProfile(profileId: Long): Flow<List<PillEntity>>

    @Query("SELECT * FROM pills WHERE profileId = :profileId ORDER BY position ASC")
    suspend fun getForProfile(profileId: Long): List<PillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pill: PillEntity): Long

    @Update
    suspend fun update(pill: PillEntity)

    @Delete
    suspend fun delete(pill: PillEntity)

    @Query("DELETE FROM pills WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface FollowedByDao {
    @Query("SELECT * FROM followed_by WHERE profileId = :profileId ORDER BY position ASC")
    fun observeForProfile(profileId: Long): Flow<List<FollowedByEntity>>

    @Query("SELECT * FROM followed_by WHERE profileId = :profileId ORDER BY position ASC")
    suspend fun getForProfile(profileId: Long): List<FollowedByEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: FollowedByEntity): Long

    @Update
    suspend fun update(entry: FollowedByEntity)

    @Query("DELETE FROM followed_by WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE profileId = :profileId ORDER BY position ASC")
    fun observeForProfile(profileId: Long): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE profileId = :profileId ORDER BY position ASC")
    suspend fun getForProfile(profileId: Long): List<HighlightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(highlight: HighlightEntity): Long

    @Update
    suspend fun update(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE profileId = :profileId ORDER BY position ASC")
    fun observeForProfile(profileId: Long): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE profileId = :profileId ORDER BY position ASC")
    suspend fun getForProfile(profileId: Long): List<PostEntity>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getById(id: Long): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity): Long

    @Update
    suspend fun update(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CarouselItemDao {
    @Query("SELECT * FROM carousel_items WHERE postId = :postId ORDER BY position ASC")
    fun observeForPost(postId: Long): Flow<List<CarouselItemEntity>>

    @Query("SELECT * FROM carousel_items WHERE postId = :postId ORDER BY position ASC")
    suspend fun getForPost(postId: Long): List<CarouselItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CarouselItemEntity): Long

    @Query("DELETE FROM carousel_items WHERE postId = :postId")
    suspend fun deleteForPost(postId: Long)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY position ASC")
    fun observeForPost(postId: Long): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY position ASC")
    suspend fun getForPost(postId: Long): List<CommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity): Long

    @Update
    suspend fun update(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY username COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
