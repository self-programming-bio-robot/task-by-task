package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.composeApp.screens.history.AssistantReviewResponse

class ReviewCache {
    private val reviews = mutableMapOf<String, AssistantReviewResponse>()

    fun getReview(reviewId: String): AssistantReviewResponse? = reviews[reviewId]

    fun addReview(reviewId: String, review: AssistantReviewResponse) {
        reviews.put(
            reviewId,
            review
        )
    }
}
