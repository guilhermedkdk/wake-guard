package io.github.guilhermedkdk.wakeguard.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.guilhermedkdk.wakeguard.R

private val ChallengeType.label
    get() =
        when (this) {
            ChallengeType.MATH -> R.string.challenge_math
            ChallengeType.MEMORY -> R.string.challenge_memory
            ChallengeType.SHAKE -> R.string.challenge_shake
            ChallengeType.TYPING -> R.string.challenge_typing
        }

private val ChallengeType.amountLabel
    get() =
        when (this) {
            ChallengeType.MATH -> R.plurals.amount_math
            ChallengeType.MEMORY -> R.plurals.amount_memory
            ChallengeType.SHAKE -> R.plurals.amount_shake
            ChallengeType.TYPING -> R.plurals.amount_typing
        }

private val Difficulty.label
    get() =
        when (this) {
            Difficulty.EASY -> R.string.difficulty_easy
            Difficulty.MEDIUM -> R.string.difficulty_medium
            Difficulty.HARD -> R.string.difficulty_hard
        }

@Composable
fun challengeTypeName(type: ChallengeType): String = stringResource(type.label)

/** One line describing a challenge, as in "Matemática · Médio · 3 contas". */
@Composable
fun challengeSummary(spec: ChallengeSpec): String =
    stringResource(
        R.string.challenge_summary,
        stringResource(spec.type.label),
        stringResource(spec.difficulty.label),
        pluralStringResource(spec.type.amountLabel, spec.amount, spec.amount),
    )

/** Survives rotation: a spec list flattened to enum ordinals and amounts. */
val ChallengeSpecListSaver =
    listSaver<List<ChallengeSpec>, Int>(
        save = { specs -> specs.flatMap { listOf(it.type.ordinal, it.difficulty.ordinal, it.amount) } },
        restore = { saved ->
            saved.chunked(3).map { (type, difficulty, amount) ->
                ChallengeSpec(ChallengeType.entries[type], Difficulty.entries[difficulty], amount)
            }
        },
    )

/** Lets the person pick the type, difficulty and amount of one challenge. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChallengeSpecDialog(
    initial: ChallengeSpec,
    onConfirm: (ChallengeSpec) -> Unit,
    onDismiss: () -> Unit,
) {
    var spec by rememberSaveable(stateSaver = SpecSaver) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.challenge_dialog_title)) },
        confirmButton = { TextButton(onClick = { onConfirm(spec) }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChallengeType.entries.forEach { type ->
                        FilterChip(
                            selected = type == spec.type,
                            onClick = { if (type != spec.type) spec = ChallengeSpec(type, spec.difficulty) },
                            label = { Text(stringResource(type.label)) },
                        )
                    }
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Difficulty.entries.forEachIndexed { index, difficulty ->
                        SegmentedButton(
                            selected = difficulty == spec.difficulty,
                            onClick = { spec = spec.copy(difficulty = difficulty) },
                            shape = SegmentedButtonDefaults.itemShape(index, Difficulty.entries.size),
                            label = { Text(stringResource(difficulty.label)) },
                        )
                    }
                }
                AmountStepper(spec) { spec = it }
            }
        },
    )
}

@Composable
private fun AmountStepper(
    spec: ChallengeSpec,
    onChange: (ChallengeSpec) -> Unit,
) {
    val amounts = spec.type.amounts
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = { onChange(spec.copy(amount = spec.amount - amounts.step)) },
            enabled = spec.amount > amounts.first,
        ) {
            Icon(painterResource(R.drawable.ic_remove), stringResource(R.string.challenge_less))
        }
        Text(
            pluralStringResource(spec.type.amountLabel, spec.amount, spec.amount),
            style = MaterialTheme.typography.titleMedium,
        )
        FilledTonalIconButton(
            onClick = { onChange(spec.copy(amount = spec.amount + amounts.step)) },
            enabled = spec.amount < amounts.last,
        ) {
            Icon(painterResource(R.drawable.ic_add), stringResource(R.string.challenge_more))
        }
    }
}

private val SpecSaver =
    listSaver<ChallengeSpec, Int>(
        save = { listOf(it.type.ordinal, it.difficulty.ordinal, it.amount) },
        restore = { (type, difficulty, amount) ->
            ChallengeSpec(ChallengeType.entries[type], Difficulty.entries[difficulty], amount)
        },
    )
