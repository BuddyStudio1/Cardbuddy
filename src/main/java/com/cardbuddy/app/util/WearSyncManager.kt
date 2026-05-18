package com.cardbuddy.app.util

import android.content.Context
import android.util.Log
import com.cardbuddy.app.model.Card
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WearSyncManager {
    private const val CARDS_PATH = "/cards"
    private const val CARDS_KEY = "cards_json"

    suspend fun syncCards(context: Context, cards: List<Card>) {
        try {
            val cardsJson = Json.encodeToString(cards)
            val putDataMapReq = PutDataMapRequest.create(CARDS_PATH).apply {
                dataMap.putString(CARDS_KEY, cardsJson)
                // Add timestamp to ensure data changes and triggers update
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }
            val putDataReq = putDataMapReq.asPutDataRequest()
            putDataReq.setUrgent()
            
            val dataClient = Wearable.getDataClient(context)
            dataClient.putDataItem(putDataReq).await()
            Log.d("WearSyncManager", "Successfully synced ${cards.size} cards to wear")
        } catch (e: Exception) {
            Log.e("WearSyncManager", "Failed to sync cards", e)
        }
    }
}
