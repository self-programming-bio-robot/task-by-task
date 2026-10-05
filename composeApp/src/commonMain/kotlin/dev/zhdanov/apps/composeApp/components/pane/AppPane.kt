package dev.zhdanov.apps.composeApp.components.pane

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

/**
 * Card-like container shared by all content panes. The whole pane slot is
 * painted with an opaque [MaterialTheme.colorScheme.surface] base — like the
 * root layout of single-pane screens — so scene transitions don't show
 * through. On compact windows the card fills the slot edge to edge; on wider
 * layouts it is a rounded card with a bottom margin. Gaps between cards come
 * from the scene's own partition spacer; outer edges are padded by the
 * scene-level container.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppPaneSurface(
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isCompact = !currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(600)
    val padding = if (isCompact) PaddingValues(0.dp)
        else PaddingValues(bottom = 16.dp)
    val shape = if (isCompact) RectangleShape else MaterialTheme.shapes.medium

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clip(shape),
            shape = shape,
            color = color,
            content = content,
        )
    }
}

/**
 * Common chrome for a content pane: an optional title header followed by
 * the pane content. Screen-level title, breadcrumbs and back navigation are
 * rendered by the scene top bar, not by the pane itself.
 */
@Composable
fun AppPane(
    title: String? = null,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit,
) {
    AppPaneSurface(
        modifier = modifier,
        color = color,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
                HorizontalDivider()
            }
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}
