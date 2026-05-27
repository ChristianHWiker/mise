package io.github.chwi.recipecalculator.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chwi.recipecalculator.ui.theme.RecipeTheme
import io.github.chwi.recipecalculator.ui.theme.SerifFamily

/**
 * Recipe detail and scaling. Shows the recipe, forward-scales via the yield stepper, and opens the
 * [ReverseScaleSheet] when an ingredient row is tapped. All state lives in [RecipeDetailViewModel];
 * this composable only renders [DetailUiState] and forwards events.
 */
@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            item { DetailTopBar(state.number, state.pinned, onBack, viewModel::togglePin) }
            item { HeroPhoto() }
            item { TitleBlock(state.category, state.title, state.metaLine) }
            item {
                ServingsCard(
                    servings = state.servings,
                    yieldUnit = state.yieldUnit,
                    onDecrement = { viewModel.changeServings(increase = false) },
                    onIncrement = { viewModel.changeServings(increase = true) },
                )
            }
            item { Hint() }
            item { SectionTitle("Ingredients") }
            items(state.ingredients) { ingredient ->
                IngredientRow(ingredient, onClick = { viewModel.openSheet(ingredient.id) })
            }
            item { Spacer(Modifier.height(RecipeTheme.spacing.huge)) }
            item { SectionTitle("Method") }
            itemsIndexedSteps(state.steps)
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    state.sheet?.let { target ->
        ReverseScaleSheet(
            target = target,
            factor = state.factor,
            fractionStyle = state.fractionStyle,
            unitSystem = state.unitSystem,
            baseServings = state.baseServings,
            yieldUnit = state.yieldUnit,
            onApply = viewModel::applyScale,
            onClose = viewModel::closeSheet,
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedSteps(steps: List<String>) {
    itemsIndexed(steps) { index, step ->
        MethodStep(number = index + 1, body = step, isLast = index == steps.lastIndex)
    }
}

@Composable
private fun DetailTopBar(number: String, pinned: Boolean, onBack: () -> Unit, onTogglePin: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RecipeTheme.spacing.xl, vertical = RecipeTheme.spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBackIos,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = "$number · COOKBOOK",
            style = RecipeTheme.typography.kicker,
            color = RecipeTheme.colors.muted,
        )
        IconButton(onClick = onTogglePin) {
            Icon(
                imageVector = if (pinned) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (pinned) "Unsave" else "Save",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun HeroPhoto() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = RecipeTheme.spacing.xxl)
            .padding(top = RecipeTheme.spacing.xs)
            .height(220.dp)
            .background(
                brush = Brush.linearGradient(listOf(Color(0xFFCD9C70), Color(0xFF6B3E22))),
                shape = RoundedCornerShape(RecipeTheme.radii.hairlineCard),
            ),
    )
}

@Composable
private fun TitleBlock(category: String, title: String, metaLine: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = RecipeTheme.spacing.huge, bottom = RecipeTheme.spacing.xxs),
    ) {
        Text(
            text = category.uppercase(),
            style = RecipeTheme.typography.kicker,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(RecipeTheme.spacing.md))
        Text(
            text = title,
            style = RecipeTheme.typography.detailH1,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(RecipeTheme.spacing.xxl))
        Text(
            text = metaLine,
            style = RecipeTheme.typography.caption,
            color = RecipeTheme.colors.muted,
        )
    }
}

@Composable
private fun ServingsCard(servings: Int, yieldUnit: String, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = RecipeTheme.spacing.xxxl)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(RecipeTheme.radii.card))
            .border(1.dp, RecipeTheme.colors.rule, RoundedCornerShape(RecipeTheme.radii.card))
            .padding(horizontal = RecipeTheme.spacing.xxl, vertical = RecipeTheme.spacing.xl),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "YIELD",
                style = RecipeTheme.typography.kicker,
                color = RecipeTheme.colors.muted,
            )
            Spacer(Modifier.height(RecipeTheme.spacing.xxs))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$servings ",
                    style = RecipeTheme.typography.sectionH2.copy(fontSize = 24.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = yieldUnit,
                    style = RecipeTheme.typography.caption.copy(fontSize = 13.sp),
                    color = RecipeTheme.colors.muted,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.md)) {
            CircleStepButton(decrement = true, onClick = onDecrement)
            CircleStepButton(decrement = false, onClick = onIncrement)
        }
    }
}

@Composable
private fun CircleStepButton(decrement: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .border(1.dp, RecipeTheme.colors.rule, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (decrement) Icons.Filled.Remove else Icons.Filled.Add,
            contentDescription = if (decrement) "Fewer" else "More",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun Hint() {
    Text(
        text = "Tap an ingredient to scale by what you have →",
        style = RecipeTheme.typography.caption.copy(
            fontFamily = SerifFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 12.sp,
        ),
        color = RecipeTheme.colors.mutedSoft,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = RecipeTheme.spacing.xl),
    )
}

@Composable
private fun SectionTitle(title: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = RecipeTheme.spacing.xxl),
    ) {
        Text(
            text = title,
            style = RecipeTheme.typography.sectionH2,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = RecipeTheme.spacing.sm),
        )
        HorizontalDivider(color = RecipeTheme.colors.rule)
    }
}

@Composable
private fun IngredientRow(ingredient: IngredientRowUi, onClick: () -> Unit) {
    Column(Modifier.padding(horizontal = 22.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = RecipeTheme.spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = ingredient.name,
                    style = RecipeTheme.typography.body,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                ingredient.modifier?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = RecipeTheme.typography.caption.copy(
                            fontFamily = SerifFamily,
                            fontStyle = FontStyle.Italic,
                        ),
                        color = RecipeTheme.colors.muted,
                    )
                }
            }
            Spacer(Modifier.width(RecipeTheme.spacing.lg))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = ingredient.qtyText,
                    style = RecipeTheme.typography.ingredientQty,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (ingredient.unitText.isNotBlank()) {
                    Text(
                        text = " ${ingredient.unitText}",
                        style = RecipeTheme.typography.caption.copy(fontSize = 13.sp),
                        color = RecipeTheme.colors.muted,
                    )
                }
            }
        }
        HorizontalDivider(color = RecipeTheme.colors.rule)
    }
}

@Composable
private fun MethodStep(number: Int, body: String, isLast: Boolean) {
    Column(Modifier.padding(horizontal = 22.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RecipeTheme.spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.xl),
        ) {
            Text(
                text = "$number",
                style = RecipeTheme.typography.stepNumber,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(22.dp),
                textAlign = TextAlign.Start,
            )
            Text(
                text = body,
                style = RecipeTheme.typography.body.copy(fontSize = 14.sp, lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
        }
        if (!isLast) HorizontalDivider(color = RecipeTheme.colors.rule)
    }
}
