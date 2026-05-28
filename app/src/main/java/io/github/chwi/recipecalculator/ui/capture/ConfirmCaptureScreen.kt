package io.github.chwi.recipecalculator.ui.capture

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chwi.recipecalculator.core.parser.ParsedLine
import io.github.chwi.recipecalculator.core.rational.Rational
import io.github.chwi.recipecalculator.ui.recipes.UNIT_CHOICES
import io.github.chwi.recipecalculator.ui.theme.RecipeTheme

private val PagePadding = 22.dp
private const val REVIEW_THRESHOLD = 0.7f

/**
 * Editable list of parsed OCR rows. The user fixes mis-parses inline; tapping save stages the rows
 * via [CaptureViewModel.stageForEditor] and the host route navigates to the recipe editor seeded
 * from the capture handoff.
 */
@Composable
fun ConfirmCaptureScreen(
    onBack: () -> Unit,
    onContinueToEditor: () -> Unit,
    viewModel: CaptureViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rows = (state.stage as? CaptureStage.Confirm)?.rows ?: emptyList()
    val needsReviewCount = rows.count { it.confidence < REVIEW_THRESHOLD }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RecipeTheme.spacing.lg, vertical = RecipeTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
            ) {
                item {
                    Column(Modifier.padding(horizontal = PagePadding)) {
                        Text(
                            text = "CONFIRM",
                            style = RecipeTheme.typography.kicker,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(RecipeTheme.spacing.sm))
                        Text(
                            text = "Check the ingredients",
                            style = RecipeTheme.typography.detailH1,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(RecipeTheme.spacing.md))
                        Text(
                            text = buildString {
                                append("${rows.size} ${if (rows.size == 1) "row" else "rows"}")
                                if (needsReviewCount > 0) append(" · $needsReviewCount need review")
                            },
                            style = RecipeTheme.typography.caption,
                            color = RecipeTheme.colors.muted,
                        )
                        Spacer(Modifier.height(RecipeTheme.spacing.xxl))
                    }
                }

                itemsIndexed(rows, key = { idx, _ -> idx }) { index, row ->
                    Column(Modifier.padding(horizontal = PagePadding)) {
                        ParsedRow(
                            row = row,
                            onChange = { transform -> viewModel.updateRow(index) { transform(it) } },
                            onRemove = { viewModel.removeRow(index) },
                        )
                        Spacer(Modifier.height(RecipeTheme.spacing.md))
                    }
                }

                if (rows.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Nothing left to confirm.",
                                style = RecipeTheme.typography.sectionH2.copy(
                                    fontSize = 14.sp,
                                    fontStyle = FontStyle.Italic,
                                ),
                                color = RecipeTheme.colors.muted,
                            )
                        }
                    }
                }
            }
        }

        // Floating primary action — same shape as the Home FAB for visual consistency.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(RecipeTheme.spacing.xxl)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 50))
                .clickable(enabled = rows.isNotEmpty()) {
                    viewModel.stageForEditor()
                    onContinueToEditor()
                }
                .padding(horizontal = RecipeTheme.spacing.xxxl, vertical = RecipeTheme.spacing.xl),
        ) {
            Text(
                text = "Save → editor",
                style = RecipeTheme.typography.body,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ParsedRow(
    row: ParsedLine,
    onChange: ((ParsedLine) -> ParsedLine) -> Unit,
    onRemove: () -> Unit,
) {
    val needsReview = row.confidence < REVIEW_THRESHOLD

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (needsReview) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                else RecipeTheme.colors.rule,
                shape = RoundedCornerShape(RecipeTheme.radii.card),
            )
            .padding(horizontal = RecipeTheme.spacing.lg, vertical = RecipeTheme.spacing.lg),
    ) {
        if (needsReview) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "NEEDS REVIEW",
                    style = RecipeTheme.typography.kicker,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove row",
                        tint = RecipeTheme.colors.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(RecipeTheme.spacing.sm))
        } else {
            Row {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove row",
                        tint = RecipeTheme.colors.mutedSoft,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            CompactField(
                value = row.qty?.toUserText().orEmpty(),
                onValueChange = { v ->
                    onChange { it.copy(qty = Rational.parseOrNull(v)) }
                },
                placeholder = "qty",
                modifier = Modifier.width(72.dp),
            )
            Spacer(Modifier.width(RecipeTheme.spacing.sm))
            UnitDropdown(
                value = row.unit.orEmpty(),
                onValueChange = { v -> onChange { it.copy(unit = v.ifEmpty { null }) } },
            )
        }
        Spacer(Modifier.height(RecipeTheme.spacing.sm))
        CompactField(
            value = row.name,
            onValueChange = { v -> onChange { it.copy(name = v) } },
            placeholder = "name",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(RecipeTheme.spacing.sm))
        CompactField(
            value = row.modifier.orEmpty(),
            onValueChange = { v -> onChange { it.copy(modifier = v.ifEmpty { null }) } },
            placeholder = "modifier (optional)",
            modifier = Modifier.fillMaxWidth(),
        )

        if (row.rawText.isNotBlank()) {
            Spacer(Modifier.height(RecipeTheme.spacing.sm))
            Text(
                text = "“${row.rawText}”",
                style = RecipeTheme.typography.caption.copy(
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                ),
                color = RecipeTheme.colors.mutedSoft,
            )
        }
    }
}

@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = RecipeTheme.typography.body.copy(fontSize = 13.sp)) },
        singleLine = true,
        textStyle = RecipeTheme.typography.body.copy(fontSize = 13.sp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier,
    )
}

@Composable
private fun UnitDropdown(
    value: String,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .border(1.dp, RecipeTheme.colors.rule, RoundedCornerShape(RecipeTheme.radii.hairlineCard))
                .clickable { expanded = true }
                .padding(horizontal = RecipeTheme.spacing.lg, vertical = RecipeTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value.ifEmpty { "—" },
                style = RecipeTheme.typography.body.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(RecipeTheme.spacing.xs))
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = RecipeTheme.colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("—", style = RecipeTheme.typography.body) },
                onClick = {
                    onValueChange("")
                    expanded = false
                },
            )
            HorizontalDivider(color = RecipeTheme.colors.rule)
            UNIT_CHOICES.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice, style = RecipeTheme.typography.body) },
                    onClick = {
                        onValueChange(choice)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Render a [Rational] back to a user-typeable string ("1 1/2", "3/4", "2"). */
private fun Rational.toUserText(): String = when {
    den == 1 -> num.toString()
    num > den -> {
        val whole = num / den
        val rem = num - whole * den
        if (rem == 0) whole.toString() else "$whole $rem/$den"
    }
    else -> "$num/$den"
}
