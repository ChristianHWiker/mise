package io.github.chwi.recipecalculator.ui.recipes

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.chwi.recipecalculator.ui.theme.RecipeTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File
import java.util.UUID

private const val ING_KEY_PREFIX = "ing-"
private const val STEP_KEY_PREFIX = "step-"
private const val SIDE_PADDING = 22

/**
 * Create/edit a recipe. Header fields, structured ingredient editor (qty parsed via
 * [io.github.chwi.recipecalculator.core.rational.Rational.parseOrNull], unit dropdown, name,
 * modifier), and an ordered steps list. Drag-to-reorder is provided by the `sh.calvin.reorderable`
 * library on a single outer `LazyColumn`; the onMove callback routes by key prefix so drags can't
 * cross between ingredient and step sections.
 */
@Composable
fun RecipeEditorScreen(
    recipeId: Long?,
    onSaved: (Long) -> Unit,
    onDeleted: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeEditorViewModel = hiltViewModel(),
) {
    @Suppress("UNUSED_PARAMETER") val _id = recipeId // route param consumed by the VM via SavedStateHandle
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.results.collect { result ->
            when (result) {
                is SaveResult.Created -> onSaved(result.id)
                is SaveResult.Updated -> onSaved(result.id)
                SaveResult.Deleted -> onDeleted()
                is SaveResult.Error -> errorMessage = result.message
            }
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { picked: Uri? ->
        if (picked != null) {
            val saved = copyImageToInternalStorage(context, picked)
            viewModel.onPhotoUriChange(saved?.toString())
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState
        if (fromKey.startsWith(ING_KEY_PREFIX) && toKey.startsWith(ING_KEY_PREFIX)) {
            val fromIdx = state.ingredients.indexOfFirst { "$ING_KEY_PREFIX${it.key}" == fromKey }
            val toIdx = state.ingredients.indexOfFirst { "$ING_KEY_PREFIX${it.key}" == toKey }
            if (fromIdx >= 0 && toIdx >= 0) viewModel.moveIngredient(fromIdx, toIdx)
        } else if (fromKey.startsWith(STEP_KEY_PREFIX) && toKey.startsWith(STEP_KEY_PREFIX)) {
            val fromIdx = state.steps.indexOfFirst { "$STEP_KEY_PREFIX${it.key}" == fromKey }
            val toIdx = state.steps.indexOfFirst { "$STEP_KEY_PREFIX${it.key}" == toKey }
            if (fromIdx >= 0 && toIdx >= 0) viewModel.moveStep(fromIdx, toIdx)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item("topbar") {
                EditorTopBar(
                    isEdit = state.isEdit,
                    saving = state.saving,
                    onCancel = onCancel,
                    onSave = viewModel::save,
                    onDelete = { showDeleteConfirm = true },
                )
            }
            item("photo") {
                PhotoBlock(
                    uri = state.photoUri,
                    onPick = {
                        photoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onClear = { viewModel.onPhotoUriChange(null) },
                )
            }
            item("title-block") {
                TitleBlock(
                    title = state.title,
                    titleError = state.errors.title,
                    category = state.category,
                    onTitleChange = viewModel::onTitleChange,
                    onCategoryChange = viewModel::onCategoryChange,
                )
            }
            item("meta") {
                MetaRow(
                    timeMinutes = state.timeMinutes,
                    timeError = state.errors.timeMinutes,
                    difficulty = state.difficulty,
                    servings = state.servings,
                    servingsError = state.errors.servings,
                    yieldUnit = state.yieldUnit,
                    onTimeChange = viewModel::onTimeChange,
                    onDifficultyChange = viewModel::onDifficultyChange,
                    onServingsChange = viewModel::onServingsChange,
                    onYieldUnitChange = viewModel::onYieldUnitChange,
                )
            }
            item("pin") {
                PinSwitch(state.pinned, onChange = viewModel::onPinnedChange)
            }
            item("tags") {
                TagsField(state.tagsInput, onChange = viewModel::onTagsChange)
            }
            item("ingredients-header") {
                SectionHeader(
                    title = "Ingredients",
                    error = if (state.errors.noValidIngredient) "At least one ingredient needs a quantity and name" else null,
                    onAdd = viewModel::addIngredient,
                )
            }
            items(state.ingredients, key = { "$ING_KEY_PREFIX${it.key}" }) { row ->
                ReorderableItem(reorderState, key = "$ING_KEY_PREFIX${row.key}") { _ ->
                    IngredientRowEditor(
                        row = row,
                        error = row.key in state.errors.ingredientKeys,
                        dragHandleModifier = Modifier.draggableHandle(),
                        onChange = { transform -> viewModel.updateIngredient(row.key, transform) },
                        onRemove = { viewModel.removeIngredient(row.key) },
                    )
                }
            }
            item("steps-header") {
                SectionHeader(title = "Method", error = null, onAdd = viewModel::addStep)
            }
            itemsIndexed(
                items = state.steps,
                key = { _, step -> "$STEP_KEY_PREFIX${step.key}" },
            ) { index, step ->
                ReorderableItem(reorderState, key = "$STEP_KEY_PREFIX${step.key}") { _ ->
                    StepRowEditor(
                        index = index,
                        step = step,
                        dragHandleModifier = Modifier.draggableHandle(),
                        onChange = { viewModel.updateStep(step.key, it) },
                        onRemove = { viewModel.removeStep(step.key) },
                    )
                }
            }
            item("save-cta") {
                BottomCtas(
                    isEdit = state.isEdit,
                    saving = state.saving,
                    onSave = viewModel::save,
                    onCancel = onCancel,
                )
            }
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            onConfirm = {
                showDeleteConfirm = false
                viewModel.delete()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } },
            title = { Text("Something went wrong") },
            text = { Text(msg) },
        )
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────────

@Composable
private fun EditorTopBar(
    isEdit: Boolean,
    saving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RecipeTheme.spacing.xl, vertical = RecipeTheme.spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCancel) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBackIos,
                contentDescription = "Discard changes",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = if (isEdit) "EDIT · COOKBOOK" else "NEW · COOKBOOK",
            style = RecipeTheme.typography.kicker,
            color = RecipeTheme.colors.muted,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(RecipeTheme.spacing.sm))
            }
            TextButton(onClick = onSave, enabled = !saving) {
                Text(
                    text = "SAVE",
                    style = RecipeTheme.typography.kicker,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (isEdit) {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Delete recipe") },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
    }
}

// ── Photo block ──────────────────────────────────────────────────────────────

@Composable
private fun PhotoBlock(uri: String?, onPick: () -> Unit, onClear: () -> Unit) {
    val shape = RoundedCornerShape(RecipeTheme.radii.hairlineCard)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RecipeTheme.spacing.xxl)
            .padding(top = RecipeTheme.spacing.xs)
            .height(180.dp)
            .clip(shape)
            .background(
                brush = Brush.linearGradient(listOf(Color(0xFFCD9C70), Color(0xFF6B3E22))),
                shape = shape,
            )
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = if (uri == null) "TAP TO CHOOSE A PHOTO" else "TAP TO REPLACE",
            style = RecipeTheme.typography.kicker,
            color = Color.White,
            modifier = Modifier.alpha(0.9f),
        )
        if (uri != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(RecipeTheme.spacing.sm),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove photo",
                    tint = Color.White,
                )
            }
        }
    }
}

// ── Title + category ─────────────────────────────────────────────────────────

@Composable
private fun TitleBlock(
    title: String,
    titleError: Boolean,
    category: String,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE_PADDING.dp)
            .padding(top = RecipeTheme.spacing.huge),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            isError = titleError,
            supportingText = if (titleError) { { Text("A title is required") } } else null,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(RecipeTheme.spacing.md))
        OutlinedTextField(
            value = category,
            onValueChange = onCategoryChange,
            label = { Text("Category") },
            placeholder = { Text("Cookies, Pasta, Bread, …") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Meta row: time · difficulty · servings · yield unit ──────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetaRow(
    timeMinutes: String,
    timeError: Boolean,
    difficulty: String,
    servings: String,
    servingsError: Boolean,
    yieldUnit: String,
    onTimeChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit,
    onServingsChange: (String) -> Unit,
    onYieldUnitChange: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE_PADDING.dp)
            .padding(top = RecipeTheme.spacing.md),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.md)) {
            OutlinedTextField(
                value = timeMinutes,
                onValueChange = onTimeChange,
                label = { Text("Time (min)") },
                isError = timeError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            DropdownPicker(
                label = "Difficulty",
                value = difficulty,
                options = DIFFICULTIES,
                onChange = onDifficultyChange,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(RecipeTheme.spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.md)) {
            OutlinedTextField(
                value = servings,
                onValueChange = onServingsChange,
                label = { Text("Servings") },
                isError = servingsError,
                supportingText = if (servingsError) { { Text("≥ 1 required") } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = yieldUnit,
                onValueChange = onYieldUnitChange,
                label = { Text("Yield unit") },
                placeholder = { Text("cookies, loaves, …") },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Pin + tags ──────────────────────────────────────────────────────────────

@Composable
private fun PinSwitch(pinned: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE_PADDING.dp)
            .padding(top = RecipeTheme.spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "PINNED",
                style = RecipeTheme.typography.kicker,
                color = RecipeTheme.colors.muted,
            )
            Spacer(Modifier.height(RecipeTheme.spacing.xxs))
            Text(
                "Show this recipe in the Pinned filter",
                style = RecipeTheme.typography.caption,
                color = RecipeTheme.colors.muted,
            )
        }
        Switch(checked = pinned, onCheckedChange = onChange)
    }
}

@Composable
private fun TagsField(value: String, onChange: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE_PADDING.dp)
            .padding(top = RecipeTheme.spacing.xl),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text("Tags") },
            placeholder = { Text("weeknight, bake, holiday") },
            supportingText = { Text("Comma-separated") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Section header ──────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, error: String?, onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE_PADDING.dp)
            .padding(top = RecipeTheme.spacing.huge),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                title,
                style = RecipeTheme.typography.sectionH2,
                color = MaterialTheme.colorScheme.onBackground,
            )
            TextButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(RecipeTheme.spacing.xxs))
                Text(
                    "Add",
                    style = RecipeTheme.typography.kicker,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (error != null) {
            Text(
                error,
                style = RecipeTheme.typography.caption,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = RecipeTheme.spacing.xxs),
            )
        }
        Spacer(Modifier.height(RecipeTheme.spacing.sm))
    }
}

// ── Ingredient row ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientRowEditor(
    row: IngredientDraft,
    error: Boolean,
    dragHandleModifier: Modifier,
    onChange: ((IngredientDraft) -> IngredientDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE_PADDING.dp)
            .padding(vertical = RecipeTheme.spacing.xs)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(RecipeTheme.radii.card))
            .border(
                width = 1.dp,
                color = if (error) MaterialTheme.colorScheme.error else RecipeTheme.colors.rule,
                shape = RoundedCornerShape(RecipeTheme.radii.card),
            )
            .padding(RecipeTheme.spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(modifier = dragHandleModifier, onClick = {}) {
                Icon(
                    Icons.Filled.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove ingredient",
                    tint = RecipeTheme.colors.muted,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = row.qtyText,
                onValueChange = { v -> onChange { it.copy(qtyText = v) } },
                label = { Text("Qty") },
                placeholder = { Text("1 1/2") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Spacer(Modifier.width(RecipeTheme.spacing.sm))
            DropdownPicker(
                label = "Unit",
                value = row.unit,
                options = UNIT_CHOICES,
                onChange = { unit -> onChange { it.copy(unit = unit) } },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(RecipeTheme.spacing.xs))
        OutlinedTextField(
            value = row.name,
            onValueChange = { v -> onChange { it.copy(name = v) } },
            label = { Text("Name") },
            placeholder = { Text("flour") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(RecipeTheme.spacing.xs))
        OutlinedTextField(
            value = row.modifier,
            onValueChange = { v -> onChange { it.copy(modifier = v) } },
            label = { Text("Modifier (optional)") },
            placeholder = { Text("cooled, packed, …") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (row.unit in setOf("cup", "tbsp", "tsp") && row.gramsPerCup != null) {
            Text(
                "≈ ${row.gramsPerCup} g/cup",
                style = RecipeTheme.typography.caption,
                color = RecipeTheme.colors.muted,
                modifier = Modifier.padding(top = RecipeTheme.spacing.xxs),
            )
        }
    }
}

// ── Step row ────────────────────────────────────────────────────────────────

@Composable
private fun StepRowEditor(
    index: Int,
    step: StepDraft,
    dragHandleModifier: Modifier,
    onChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE_PADDING.dp)
            .padding(vertical = RecipeTheme.spacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        IconButton(
            onClick = {},
            modifier = dragHandleModifier.padding(top = 8.dp),
        ) {
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = "${index + 1}",
            style = RecipeTheme.typography.stepNumber,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .width(22.dp)
                .padding(top = 14.dp),
        )
        Spacer(Modifier.width(RecipeTheme.spacing.sm))
        OutlinedTextField(
            value = step.text,
            onValueChange = onChange,
            label = { Text("Step") },
            modifier = Modifier.weight(1f),
            minLines = 1,
            maxLines = 6,
        )
        IconButton(onClick = onRemove, modifier = Modifier.padding(top = 6.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove step",
                tint = RecipeTheme.colors.muted,
            )
        }
    }
}

// ── Bottom CTAs ─────────────────────────────────────────────────────────────

@Composable
private fun BottomCtas(isEdit: Boolean, saving: Boolean, onSave: () -> Unit, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SIDE_PADDING.dp)
            .padding(top = RecipeTheme.spacing.huge),
        horizontalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.md),
    ) {
        if (isEdit) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = CircleShape,
            ) { Text("Cancel") }
        }
        Button(
            onClick = onSave,
            enabled = !saving,
            modifier = Modifier.weight(if (isEdit) 2f else 1f),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(if (isEdit) "Save changes" else "Save recipe")
        }
    }
}

// ── Confirmation dialog ─────────────────────────────────────────────────────

@Composable
private fun DeleteConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete recipe?") },
        text = { Text("This removes the recipe and all of its ingredients and steps. This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep") } },
    )
}

// ── Dropdown picker ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownPicker(
    label: String,
    value: String,
    options: List<String>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ── Photo import ────────────────────────────────────────────────────────────

/**
 * Copy the picker-supplied content URI into app-internal storage and return a `file://` URI
 * the app can read after the picker grant expires. Returns null on I/O failure.
 */
private fun copyImageToInternalStorage(context: Context, source: Uri): Uri? {
    return try {
        val dir = File(context.filesDir, "recipe_photos").apply { mkdirs() }
        val target = File(dir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        Uri.fromFile(target)
    } catch (_: Throwable) {
        null
    }
}
