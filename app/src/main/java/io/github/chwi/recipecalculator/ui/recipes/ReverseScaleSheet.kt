package io.github.chwi.recipecalculator.ui.recipes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.chwi.recipecalculator.core.rational.FractionStyle
import io.github.chwi.recipecalculator.core.rational.Rational
import io.github.chwi.recipecalculator.core.units.UnitSystem
import io.github.chwi.recipecalculator.core.units.displayQty
import io.github.chwi.recipecalculator.core.units.factorFromDisplayValue
import io.github.chwi.recipecalculator.core.units.formatStepperValue
import io.github.chwi.recipecalculator.core.units.stepFor
import io.github.chwi.recipecalculator.core.units.unitLabel
import io.github.chwi.recipecalculator.ui.theme.RecipeTheme
import io.github.chwi.recipecalculator.ui.theme.SerifFamily
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * The reverse-scale modal — the product's signature interaction. The user says how much of one
 * ingredient they actually have; [factorFromDisplayValue] turns that into a recipe-wide scale
 * factor, previewed live as a new yield, and applied on confirm.
 *
 * All quantity math runs through the tested helpers in
 * [io.github.chwi.recipecalculator.core.units], so the sheet itself stays presentational.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReverseScaleSheet(
    target: SheetTarget,
    factor: Double,
    fractionStyle: FractionStyle,
    unitSystem: UnitSystem,
    baseServings: Int,
    yieldUnit: String,
    onApply: (newFactor: Double) -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val current = displayQty(
        baseQty = target.baseQty,
        unit = target.unit,
        gramsPerCup = target.gramsPerCup,
        factor = factor,
        fractionStyle = fractionStyle,
        system = unitSystem,
    )
    val unit = current.unit
    val step = stepFor(unit)

    var value by remember(target.ingredientId) { mutableDoubleStateOf(current.qtyValue) }

    val newFactor = factorFromDisplayValue(
        baseQty = target.baseQty,
        unit = target.unit,
        gramsPerCup = target.gramsPerCup,
        currentFactor = factor,
        fractionStyle = fractionStyle,
        system = unitSystem,
        newDisplayValue = value,
    )
    val newServings = max(1, (baseServings * newFactor).roundToInt())

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
    ) {
        // Signature 4 dp accent top border, then the drag handle.
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = RecipeTheme.spacing.xl, bottom = RecipeTheme.spacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .background(RecipeTheme.colors.mutedSoft, RoundedCornerShape(2.dp)),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = RecipeTheme.spacing.xxxl),
        ) {
            Text(
                text = "REVERSE SCALE",
                style = RecipeTheme.typography.kicker.copy(letterSpacing = 0.16.em),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(RecipeTheme.spacing.xs))
            Text(
                text = "How much ${target.name.lowercase()} do you have?",
                style = RecipeTheme.typography.sheetTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(RecipeTheme.spacing.sm))
            HelperText(current.qtyText, current.unitText)
            Spacer(Modifier.height(RecipeTheme.spacing.xxl))

            StepperCard(
                valueText = formatStepperValue(value, unit, fractionStyle),
                unitLabel = unitLabel(unit, Rational.of((value * 1000).roundToLong(), 1000L)),
                onDecrement = { value = max(0.0, ((value - step) * 1000).roundToLong() / 1000.0) },
                onIncrement = { value = ((value + step) * 1000).roundToLong() / 1000.0 },
            )
            Spacer(Modifier.height(RecipeTheme.spacing.xl))

            YieldPreview(servings = newServings, yieldUnit = yieldUnit)
            Spacer(Modifier.height(RecipeTheme.spacing.xxl))

            Row(horizontalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.md)) {
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(RecipeTheme.radii.hairlineCard),
                    border = BorderStroke(1.dp, RecipeTheme.colors.rule),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        "Cancel",
                        style = RecipeTheme.typography.body,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Button(
                    onClick = { onApply(newFactor) },
                    shape = RoundedCornerShape(RecipeTheme.radii.hairlineCard),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.weight(2f),
                ) {
                    Text("Scale recipe", style = RecipeTheme.typography.body)
                }
            }
        }
    }
}

@Composable
private fun HelperText(qtyText: String, unitText: String) {
    Text(
        text = buildAnnotatedString {
            append("Recipe calls for ")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontFamily = SerifFamily, fontSize = 15.sp)) {
                append(listOf(qtyText, unitText).filter { it.isNotBlank() }.joinToString(" "))
            }
            append(". We'll scale everything else to match.")
        },
        style = RecipeTheme.typography.body.copy(fontSize = 13.sp),
        color = RecipeTheme.colors.muted,
    )
}

@Composable
private fun StepperCard(
    valueText: String,
    unitLabel: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RecipeTheme.colors.accentSoft, RoundedCornerShape(RecipeTheme.radii.hairlineCard))
            .padding(horizontal = RecipeTheme.spacing.md, vertical = RecipeTheme.spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(decrement = true, onClick = onDecrement)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = valueText,
                style = RecipeTheme.typography.stepperValue,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (unitLabel.isNotBlank()) {
                Spacer(Modifier.height(RecipeTheme.spacing.xxs))
                Text(
                    text = unitLabel.uppercase(),
                    style = RecipeTheme.typography.caption.copy(fontSize = 11.sp, letterSpacing = 0.08.em),
                    color = RecipeTheme.colors.muted,
                )
            }
        }
        StepperButton(decrement = false, onClick = onIncrement)
    }
}

@Composable
private fun StepperButton(decrement: Boolean, onClick: () -> Unit) {
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
            contentDescription = if (decrement) "Less" else "More",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun YieldPreview(servings: Int, yieldUnit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(RecipeTheme.radii.hairlineCard))
            .border(1.dp, RecipeTheme.colors.rule, RoundedCornerShape(RecipeTheme.radii.hairlineCard))
            .padding(horizontal = RecipeTheme.spacing.xl, vertical = RecipeTheme.spacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "YIELDS",
            style = RecipeTheme.typography.caption.copy(fontSize = 12.sp, letterSpacing = 0.08.em),
            color = RecipeTheme.colors.muted,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$servings ",
                style = RecipeTheme.typography.sectionH2.copy(fontSize = 20.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = yieldUnit,
                style = RecipeTheme.typography.caption.copy(fontSize = 13.sp),
                color = RecipeTheme.colors.muted,
            )
        }
    }
}
