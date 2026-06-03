package io.github.chwi.recipecalculator.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.chwi.recipecalculator.ui.theme.RecipeTheme

private val PagePadding = 22.dp

/**
 * The cookbook Home: masthead, search, tag filter, and the recipe library — the editorial direction
 * from the design handoff, driven by [HomeViewModel]. The whole page scrolls as one [LazyColumn];
 * the extended FAB floats over it.
 */
@Composable
fun RecipesListScreen(
    onRecipeClick: (Long) -> Unit,
    onAddRecipe: () -> Unit,
    onCaptureRecipe: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.searchState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { TopBar() }
            item { Masthead(state) }
            item {
                SearchField(
                    value = query,
                    onValueChange = viewModel::onSearchChange,
                    modifier = Modifier.padding(horizontal = PagePadding, vertical = RecipeTheme.spacing.xxl),
                )
            }
            item {
                TagFilterRow(
                    tags = state.tags,
                    activeTag = state.activeTag,
                    onTagSelected = viewModel::onTagSelected,
                )
            }
            item { SectionHeader(title = state.sectionTitle, count = state.sectionCount) }

            if (state.recipes.isEmpty()) {
                item { EmptyState() }
            } else {
                itemsIndexed(state.recipes, key = { _, r -> r.id }) { index, recipe ->
                    RecipeRow(
                        recipe = recipe,
                        isFirst = index == 0,
                        onClick = { onRecipeClick(recipe.id) },
                    )
                }
            }
            item { Spacer(Modifier.height(92.dp)) }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(RecipeTheme.spacing.xxl),
            horizontalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CaptureFab(onClick = onCaptureRecipe)
            ExtendedFloatingActionButton(
                onClick = onAddRecipe,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add recipe", style = RecipeTheme.typography.body) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(percent = 50),
            )
        }
    }
}

@Composable
private fun CaptureFab(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(percent = 50))
            .border(1.dp, RecipeTheme.colors.rule, RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.CameraAlt,
            contentDescription = "Scan recipe",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RecipeTheme.spacing.xxl, vertical = RecipeTheme.spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // TODO: wire to a navigation drawer / overflow menu in a later phase.
        IconButton(onClick = {}) {
            Icon(
                Icons.AutoMirrored.Filled.MenuOpen,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = "COOKBOOK",
            style = RecipeTheme.typography.kicker,
            color = RecipeTheme.colors.muted,
        )
        IconButton(onClick = {}) {
            Icon(
                Icons.Filled.MoreHoriz,
                contentDescription = "More",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun Masthead(state: HomeUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding)
            .padding(top = RecipeTheme.spacing.xxl, bottom = RecipeTheme.spacing.xs),
    ) {
        Text(
            text = state.dateKicker,
            style = RecipeTheme.typography.kicker,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(RecipeTheme.spacing.md))
        Text(
            text = "My cookbook",
            style = RecipeTheme.typography.mastheadH1,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(RecipeTheme.spacing.lg))
        Text(
            text = metaLine(state),
            style = RecipeTheme.typography.caption,
            color = RecipeTheme.colors.muted,
        )
    }
}

private fun metaLine(state: HomeUiState): String {
    val recipeWord = if (state.recipeCount == 1) "recipe" else "recipes"
    return buildString {
        append("${state.recipeCount} $recipeWord · ${state.pinnedCount} pinned")
        state.lastCookedLabel?.let { append(" · last cooked $it") }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(RecipeTheme.radii.hairlineCard))
            .border(1.dp, RecipeTheme.colors.rule, RoundedCornerShape(RecipeTheme.radii.hairlineCard))
            .padding(horizontal = RecipeTheme.spacing.xl, vertical = RecipeTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = RecipeTheme.colors.mutedSoft,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(RecipeTheme.spacing.md))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "Search recipes, ingredients…",
                    style = RecipeTheme.typography.body.copy(fontSize = 13.sp),
                    color = RecipeTheme.colors.mutedSoft,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = RecipeTheme.typography.body.copy(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TagFilterRow(
    tags: List<String>,
    activeTag: String,
    onTagSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = PagePadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tags.forEach { tag ->
            val selected = tag == activeTag
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(end = RecipeTheme.spacing.xxl)
                    .clickable { onTagSelected(tag) },
            ) {
                Text(
                    text = tag.uppercase(),
                    style = RecipeTheme.typography.kicker.copy(letterSpacing = 0.1.em),
                    color = if (selected) MaterialTheme.colorScheme.onBackground else RecipeTheme.colors.muted,
                    modifier = Modifier.padding(vertical = RecipeTheme.spacing.sm),
                )
                Box(
                    Modifier
                        .height(2.dp)
                        .width(if (selected) 20.dp else 0.dp)
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
                )
            }
        }
    }
    HorizontalDivider(color = RecipeTheme.colors.rule)
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding)
            .padding(top = RecipeTheme.spacing.xxxl, bottom = RecipeTheme.spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = RecipeTheme.typography.sectionH2,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "$count ${if (count == 1) "recipe" else "recipes"}",
            style = RecipeTheme.typography.caption.copy(fontSize = 11.sp),
            color = RecipeTheme.colors.muted,
        )
    }
}

@Composable
private fun RecipeRow(
    recipe: RecipeRowUi,
    isFirst: Boolean,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = PagePadding)) {
        if (isFirst) HorizontalDivider(color = RecipeTheme.colors.rule)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = RecipeTheme.spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumbnail(recipe.photoUri, recipe.toneSeed)
            Spacer(Modifier.width(RecipeTheme.spacing.xl))
            Column(Modifier.weight(1f)) {
                Text(
                    text = recipe.title,
                    style = RecipeTheme.typography.sectionH2.copy(fontSize = 17.sp, lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(RecipeTheme.spacing.xxs))
                RowMeta(recipe)
            }
            if (recipe.pinned) {
                Spacer(Modifier.width(RecipeTheme.spacing.sm))
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        HorizontalDivider(color = RecipeTheme.colors.rule)
    }
}

@Composable
private fun RowMeta(recipe: RecipeRowUi) {
    val muted = RecipeTheme.colors.muted
    val captionSmall = RecipeTheme.typography.caption.copy(fontSize = 11.sp)
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(letterSpacing = 0.08.em)) { append(recipe.category.uppercase()) }
            withStyle(SpanStyle(color = muted.copy(alpha = 0.5f))) { append("  ·  ") }
            append(recipe.yieldText)
            withStyle(SpanStyle(color = muted.copy(alpha = 0.5f))) { append("  ·  ") }
            append(recipe.timeText)
        },
        style = captionSmall,
        color = muted,
    )
}

@Composable
private fun Thumbnail(photoUri: String?, toneSeed: Int) {
    val tones = ThumbnailTones[(toneSeed % ThumbnailTones.size + ThumbnailTones.size) % ThumbnailTones.size]
    val shape = RoundedCornerShape(RecipeTheme.radii.hairlineCard)
    Box(
        Modifier
            .size(64.dp)
            .clip(shape)
            // Gradient stands in while the photo loads, or when the recipe has none.
            .background(brush = Brush.linearGradient(tones), shape = shape),
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 50.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Nothing matches that — yet.",
            style = RecipeTheme.typography.sectionH2.copy(
                fontSize = 14.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            ),
            color = RecipeTheme.colors.muted,
        )
    }
}

/** Warm gradient placeholders standing in for recipe photos until capture/import lands. */
private val ThumbnailTones = listOf(
    listOf(Color(0xFFCD9C70), Color(0xFF6B3E22)),
    listOf(Color(0xFFD97554), Color(0xFF7A3A26)),
    listOf(Color(0xFFB58454), Color(0xFF5E3A1F)),
    listOf(Color(0xFFC08555), Color(0xFF643922)),
    listOf(Color(0xFFC9925D), Color(0xFF5A3818)),
)
