package io.github.chwi.recipecalculator.ui.capture

import io.github.chwi.recipecalculator.core.parser.ParsedLine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot in-memory bridge from the capture/confirmation flow to the recipe editor.
 *
 * The editor accepts a new-recipe draft seeded from OCR without going through the database first:
 * the confirmation screen drops the parsed rows here, navigates to `RecipeEditor(fromCapture=true)`,
 * and the editor consumes them on init via [consume]. Consuming clears the slot, so a subsequent
 * blank-FAB open of the editor doesn't pick up stale data.
 *
 * Kept deliberately tiny — not a Flow, not state — because there's exactly one in-flight handoff at
 * a time and the consumer is on the same single-activity main thread.
 */
@Singleton
class CaptureHandoff @Inject constructor() {

    private var pending: List<ParsedLine>? = null

    /** Drop parsed rows for the next editor open. Overwrites any previous pending handoff. */
    fun stage(rows: List<ParsedLine>) {
        pending = rows
    }

    /** Take and clear the pending rows. Returns null if nothing was staged. */
    fun consume(): List<ParsedLine>? {
        val taken = pending
        pending = null
        return taken
    }
}
