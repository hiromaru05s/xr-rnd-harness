package com.example.notificationbridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person

/**
 * AIグラスへの通知ブリッジングを管理するヘルパー。
 *
 * NotificationCompatを使って、スマホ→グラスへの通知ブリッジングを実装する。
 * AIグラスは標準Android通知フレームワークを使用し、
 * Androidがデバイス能力に基づき表示を調整する。
 *
 * ブリッジング条件:
 * - IMPORTANCE_HIGH チャンネル
 * - タイトル非null・非空
 * - FLAG_LOCAL_ONLYでない
 * - 継続的通知でない
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"
        private const val CHANNEL_ID = "glasses_bridge_channel"
        private const val CHANNEL_NAME = "Glasses Notifications"
        private const val CHANNEL_DESCRIPTION = "AIグラスにブリッジされる通知チャンネル"
    }

    /** 結果型: 通知操作の結果をsealed classで表現 */
    sealed class NotificationResult {
        data class Success(val notificationId: Int) : NotificationResult()
        data class Error(val message: String) : NotificationResult()
    }

    /**
     * IMPORTANCE_HIGHの通知チャンネルを作成する。
     * グラスへのブリッジングにはIMPORTANCE_HIGH以上が必要。
     */
    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel created: $CHANNEL_ID (IMPORTANCE_HIGH)")
    }

    /**
     * 標準通知を発行する。
     *
     * ブリッジング条件を満たすために:
     * - IMPORTANCE_HIGH チャンネルを使用
     * - タイトルを必ず設定
     * - FLAG_LOCAL_ONLYを付けない（デフォルト）
     * - 継続的通知にしない（setOngoing(false)）
     */
    fun sendStandardNotification(
        notificationId: Int,
        title: String,
        content: String,
    ): NotificationResult {
        return try {
            // スマホ側タップ先
            val phonePendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID).apply {
                setSmallIcon(android.R.drawable.ic_dialog_info)
                setContentTitle(title) // 必須: ブリッジングにはタイトルが必要
                setContentText(content)
                setContentIntent(phonePendingIntent)
                setAutoCancel(true)
                setOngoing(false) // 継続的通知でないことを明示
                // FLAG_LOCAL_ONLYは付けない（デフォルト値: ブリッジ可能）
                priority = NotificationCompat.PRIORITY_HIGH
            }.build()

            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.d(TAG, "Standard notification sent: id=$notificationId")
            NotificationResult.Success(notificationId)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for notification", e)
            NotificationResult.Error("通知権限がありません: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
            NotificationResult.Error("通知送信失敗: ${e.message}")
        }
    }

    /**
     * MessagingStyle通知を発行する。
     * 会話形式の通知はグラスでの表示に最適。
     * ダイレクト返信対応で音声返信/スマートリプライが可能。
     */
    fun sendMessagingNotification(
        notificationId: Int,
        senderName: String,
        message: String,
    ): NotificationResult {
        return try {
            val person = Person.Builder()
                .setName(senderName)
                .build()

            val messagingStyle = NotificationCompat.MessagingStyle(person)
                .setConversationTitle("$senderName との会話")
                .addMessage(message, System.currentTimeMillis(), person)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID).apply {
                setSmallIcon(android.R.drawable.ic_dialog_email)
                setStyle(messagingStyle)
                setAutoCancel(true)
                setOngoing(false)
                priority = NotificationCompat.PRIORITY_HIGH
            }.build()

            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.d(TAG, "Messaging notification sent: id=$notificationId")
            NotificationResult.Success(notificationId)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for messaging notification", e)
            NotificationResult.Error("通知権限がありません: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send messaging notification", e)
            NotificationResult.Error("通知送信失敗: ${e.message}")
        }
    }

    /** 通知をキャンセルする */
    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
        Log.d(TAG, "Notification cancelled: id=$notificationId")
    }

    /** 全通知をキャンセルする */
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
        Log.d(TAG, "All notifications cancelled")
    }
}
