package com.humanoidai.memory.dao

import androidx.room.*
import com.humanoidai.memory.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userClass = :userClass")
    suspend fun getUsersByClass(userClass: String): List<UserEntity>

    @Query("SELECT * FROM users ORDER BY lastSeenAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY lastSeenAt DESC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE userClass = 'OWNER' LIMIT 1")
    suspend fun getOwner(): UserEntity?

    @Query("SELECT * FROM users WHERE isCriticalContact = 1")
    suspend fun getCriticalContacts(): List<UserEntity>

    @Query("UPDATE users SET lastSeenAt = :timestamp, detectionCount = detectionCount + 1 WHERE userId = :userId")
    suspend fun updateLastSeen(userId: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM users WHERE userClass != 'UNKNOWN'")
    suspend fun getRegisteredUserCount(): Int

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
