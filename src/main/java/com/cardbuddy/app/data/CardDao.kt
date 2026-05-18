package com.cardbuddy.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY usageCount DESC, createdAt DESC")
    fun getAllCardsSortedByUsage(): Flow<List<CardEntity>>

    @Insert
    suspend fun insertCard(card: CardEntity)

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    suspend fun incrementUsageCount(cardId: Long) {
        val card = getById(cardId) ?: return
        val updatedCard = card.copy(usageCount = card.usageCount + 1)
        updateCard(updatedCard)
    }

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Long): CardEntity?

    @Query("SELECT * FROM cards")
    suspend fun getAllCardsDirect(): List<CardEntity>
}