package io.github.guilhermedkdk.wakeguard.alarm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.guilhermedkdk.wakeguard.R
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeSpec
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeSpecDialog
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeSpecListSaver
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeType
import io.github.guilhermedkdk.wakeguard.challenge.challengeSummary
import io.github.guilhermedkdk.wakeguard.challenge.challengeTypeName
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

private val NEW_ALARM = Alarm(time = LocalTime.of(7, 0), challenges = listOf(ChallengeSpec(ChallengeType.MATH)))

/** Creates an alarm when [alarmId] is null, otherwise edits or deletes that one. */
@Composable
fun AlarmEditorScreen(
    book: AlarmBook,
    alarmId: Long?,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    var original by remember { mutableStateOf(if (alarmId == null) NEW_ALARM else null) }
    LaunchedEffect(alarmId) {
        if (alarmId != null) original = book.find(alarmId) ?: return@LaunchedEffect onClose()
    }
    val scope = rememberCoroutineScope()
    original?.let { alarm ->
        Editor(
            original = alarm,
            onSave = { edited -> scope.launch { book.save(edited) }.invokeOnCompletion { onClose() } },
            onDelete = { scope.launch { book.delete(alarm.id) }.invokeOnCompletion { onClose() } },
            onClose = onClose,
        )
    }
}

// Each field is saved on its own, so a rotation keeps the edits and ignores the reloaded original.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun Editor(
    original: Alarm,
    onSave: (Alarm) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    val time = rememberTimePickerState(original.time.hour, original.time.minute, is24Hour = true)
    var dayBits by rememberSaveable { mutableIntStateOf(original.days.sumOf { 1 shl it.ordinal }) }
    var name by rememberSaveable { mutableStateOf(original.name) }
    var vibrate by rememberSaveable { mutableStateOf(original.vibrate) }
    var risingVolume by rememberSaveable { mutableStateOf(original.risingVolume) }
    var muteDuringChallenge by rememberSaveable { mutableStateOf(original.muteDuringChallenge) }
    var challenges by rememberSaveable(stateSaver = ChallengeSpecListSaver) { mutableStateOf(original.challenges) }
    // Index of the challenge in the dialog; the list size means a new one.
    var dialogIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (original.id == 0L) R.string.alarm_new else R.string.alarm_edit)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(R.drawable.ic_back), stringResource(R.string.alarm_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSave(
                                original.copy(
                                    time = LocalTime.of(time.hour, time.minute),
                                    days =
                                        DayOfWeek.entries.filterTo(mutableSetOf()) {
                                            dayBits and (1 shl it.ordinal) !=
                                                0
                                        },
                                    name = name.trim(),
                                    vibrate = vibrate,
                                    risingVolume = risingVolume,
                                    challenges = challenges,
                                    muteDuringChallenge = muteDuringChallenge,
                                ),
                            )
                        },
                    ) { Text(stringResource(R.string.alarm_save)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TimeInput(time, Modifier.align(Alignment.CenterHorizontally))

            Text(stringResource(R.string.alarm_days), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val bit = 1 shl day.ordinal
                    FilterChip(
                        selected = dayBits and bit != 0,
                        onClick = { dayBits = dayBits xor bit },
                        label = { Text(stringResource(day.label)) },
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= Alarm.MAX_NAME_LENGTH) name = it },
                label = { Text(stringResource(R.string.alarm_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SwitchRow(stringResource(R.string.alarm_vibrate), null, vibrate) { vibrate = it }
            SwitchRow(
                stringResource(R.string.alarm_rising_volume),
                stringResource(R.string.alarm_rising_volume_hint),
                risingVolume,
            ) { risingVolume = it }
            SwitchRow(
                stringResource(R.string.alarm_mute_during_challenge),
                stringResource(R.string.alarm_mute_during_challenge_hint),
                muteDuringChallenge,
            ) { muteDuringChallenge = it }

            Text(stringResource(R.string.alarm_challenges), style = MaterialTheme.typography.titleMedium)
            challenges.forEachIndexed { index, spec ->
                ChallengeRow(
                    spec = spec,
                    canMoveUp = index > 0,
                    canMoveDown = index < challenges.lastIndex,
                    canRemove = challenges.size > 1,
                    onClick = { dialogIndex = index },
                    onMove = { offset -> challenges = challenges.swapped(index, index + offset) },
                    onRemove = { challenges = challenges.filterIndexed { i, _ -> i != index } },
                )
            }
            OutlinedButton(
                onClick = { dialogIndex = challenges.size },
                enabled = challenges.size < Alarm.MAX_CHALLENGES,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.alarm_add_challenge)) }

            if (original.id != 0L) {
                TextButton(onClick = { confirmingDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.alarm_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    dialogIndex?.let { index ->
        ChallengeSpecDialog(
            initial = challenges.getOrElse(index) { ChallengeSpec(ChallengeType.MATH) },
            onConfirm = { spec ->
                challenges = if (index < challenges.size) challenges.replaced(index, spec) else challenges + spec
                dialogIndex = null
            },
            onDismiss = { dialogIndex = null },
        )
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.alarm_delete_title)) },
            confirmButton = {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.alarm_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    hint: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ChallengeRow(
    spec: ChallengeSpec,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val type = challengeTypeName(spec.type)
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(challengeSummary(spec), Modifier.weight(1f))
            IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                Icon(painterResource(R.drawable.ic_arrow_up), stringResource(R.string.challenge_move_up, type))
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                Icon(painterResource(R.drawable.ic_arrow_down), stringResource(R.string.challenge_move_down, type))
            }
            IconButton(onClick = onRemove, enabled = canRemove) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.challenge_remove, type))
            }
        }
    }
}

private fun <T> List<T>.replaced(
    index: Int,
    value: T,
) = mapIndexed { i, old -> if (i == index) value else old }

private fun <T> List<T>.swapped(
    a: Int,
    b: Int,
) = toMutableList().also {
    it[a] = this[b]
    it[b] = this[a]
}
