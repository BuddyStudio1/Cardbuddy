package com.cardbuddy.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.cardbuddy.app.MainActivity
import com.cardbuddy.app.R
import com.cardbuddy.app.data.CardEntity

object ShortcutManagerHelper {
    fun updateShortcuts(context: Context, cards: List<CardEntity>) {
        val shortcuts = cards
            .sortedByDescending { it.usageCount }
            .take(4)
            .map { createShortcutInfo(context, it) }

        try {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (e: Exception) {
            // Log or ignore if too many shortcuts or other issues
        }
    }

    fun pinShortcut(context: Context, card: CardEntity) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            val pinShortcutInfo = createShortcutInfo(context, card)
            ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null)
        }
    }

    private fun createShortcutInfo(context: Context, card: CardEntity): ShortcutInfoCompat {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("card_id", card.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return ShortcutInfoCompat.Builder(context, "card_${card.id}")
            .setShortLabel(card.storeName)
            .setLongLabel(card.storeName)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.cardbuddy_launcher))
            .setIntent(intent)
            .build()
    }
}