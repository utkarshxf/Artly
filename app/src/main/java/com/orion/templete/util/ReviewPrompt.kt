package com.orion.templete.util

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Asks for a Play Store review once, after the user's 5th like or first follow.
 * The system dialog only appears for Play Store installs; in debug builds the
 * call is a no-op.
 */
object ReviewPrompt {
    private const val LIKE_THRESHOLD = 5

    fun maybeRequestReview(activity: Activity, storage: SecureStorage) {
        if (storage.wasReviewPromptShown()) return
        if (storage.getLikeCount() < LIKE_THRESHOLD && !storage.hasFollowedOnce()) return
        storage.setReviewPromptShown()
        try {
            val manager = ReviewManagerFactory.create(activity)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    manager.launchReviewFlow(activity, task.result)
                }
            }
        } catch (e: Exception) {
            Log.w("ReviewPrompt", "review flow unavailable", e)
        }
    }
}
