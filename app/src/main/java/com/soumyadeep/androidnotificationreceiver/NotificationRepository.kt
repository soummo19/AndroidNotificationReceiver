package com.soumyadeep.androidnotificationreceiver

import android.util.Log
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class ReceivedNotification(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

object NotificationRepository {

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _installationId = MutableStateFlow<String?>(null)
    val installationId: StateFlow<String?> = _installationId.asStateFlow()

    private val _lastReceivedNotification = MutableStateFlow<ReceivedNotification?>(null)
    val lastReceivedNotification: StateFlow<ReceivedNotification?> = _lastReceivedNotification.asStateFlow()

    private val _isLoadingToken = MutableStateFlow(false)
    val isLoadingToken: StateFlow<Boolean> = _isLoadingToken.asStateFlow()

    fun updateToken(token: String) {
        _fcmToken.value = token
    }

    fun updateInstallationId(installationId: String) {
        _installationId.value = installationId
    }

    fun onNotificationReceived(notification: ReceivedNotification) {
        _lastReceivedNotification.value = notification
    }

    fun clearLastNotification() {
        _lastReceivedNotification.value = null
    }

    suspend fun fetchFcmToken(): Result<String> {
        _isLoadingToken.value = true
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            _fcmToken.value = token
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isLoadingToken.value = false
        }
    }

    suspend fun refreshFcmToken(): Result<String> {
        _isLoadingToken.value = true
        return try {
            // Delete existing registration token to force a fresh token generation
            FirebaseMessaging.getInstance().deleteToken().await()
            val newToken = FirebaseMessaging.getInstance().token.await()
            _fcmToken.value = newToken
            Result.success(newToken)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isLoadingToken.value = false
        }
    }

    suspend fun fetchInstallationId(): Result<String> {
        return try {
            val fid = FirebaseInstallations.getInstance().id.await()
            Log.d("FCM_DEBUG", "Successfully fetched Firebase Installation ID (FID): $fid")
            _installationId.value = fid
            Result.success(fid)
        } catch (e: Exception) {
            Log.e("FCM_DEBUG", "Failed to fetch Firebase Installation ID (FID)", e)
            Result.failure(e)
        }
    }
}
