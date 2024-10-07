package dev.zhdanov.apps.composeApp.screens.finishedDay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import dev.zhdanov.apps.composeApp.services.ReviewCache
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun FinishedDayScreen(
    reviewId: String?,
    onNext: () -> Unit
) {
    val reviewCache = koinInject<ReviewCache>()
    val review = reviewId?.let { reviewCache.getReview(reviewId) }

    if (review == null) {
        onNext()
        return
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Review:", style = MaterialTheme.typography.headlineMedium)
            Markdown(review.summary, modifier = Modifier.wrapContentHeight())
            Spacer(modifier = Modifier.height(16.dp))
            Text("Buddy:", style = MaterialTheme.typography.headlineMedium)
            Markdown(review.response, modifier = Modifier.wrapContentHeight())
            Button(
                onClick = { onNext() },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            ) {
                Text("Continue")
            }
        }
    }
}
