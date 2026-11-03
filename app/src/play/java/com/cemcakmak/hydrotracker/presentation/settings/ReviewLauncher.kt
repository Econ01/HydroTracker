package com.cemcakmak.hydrotracker.presentation.settings

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.cemcakmak.hydrotracker.R
import com.google.android.play.core.review.ReviewManagerFactory

/** Launches Google Play's review flow in the Play distribution flavour. */
fun launchInAppReview(context: Context) {
    val manager = ReviewManagerFactory.create(context)
    val request = manager.requestReviewFlow()

    request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Toast.makeText(context, context.getString(R.string.toast_review_successful), Toast.LENGTH_SHORT).show()
            val reviewInfo = task.result
            val flow = manager.launchReviewFlow(context as Activity, reviewInfo)
            flow.addOnCompleteListener {
                // The API does not report whether the user actually left a review.
            }
        } else {
            Toast.makeText(context, context.getString(R.string.toast_review_failed), Toast.LENGTH_SHORT).show()
        }
    }
}
