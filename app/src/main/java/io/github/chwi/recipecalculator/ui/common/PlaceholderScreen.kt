package io.github.chwi.recipecalculator.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import io.github.chwi.recipecalculator.ui.theme.RecipeTheme

/**
 * A temporary screen body: a terracotta kicker over a serif title and a muted note, rendered with
 * the editorial tokens so each placeholder confirms the theme is wired correctly. Replaced by the
 * real screens as later phases land.
 */
@Composable
fun PlaceholderScreen(
    kicker: String,
    title: String,
    note: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(RecipeTheme.spacing.huge),
        verticalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = kicker.uppercase(),
            style = RecipeTheme.typography.kicker,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = RecipeTheme.typography.detailH1,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = note,
            style = RecipeTheme.typography.body,
            color = RecipeTheme.colors.muted,
            textAlign = TextAlign.Center,
        )
    }
}
