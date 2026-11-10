package io.github.guilhermedkdk.wakeguard.alarm

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.guilhermedkdk.wakeguard.R
import io.github.guilhermedkdk.wakeguard.challenge.challengeSummary
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

/** The Alarmes tab: every alarm, with a switch each and a button to create one. */
@Composable
fun AlarmsScreen(
    book: AlarmBook,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Null until the first read, so the empty message never flashes over real alarms.
    val alarms by book.alarms.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    Box(modifier.fillMaxSize()) {
        when {
            alarms == null -> {}

            alarms!!.isEmpty() -> {
                Text(stringResource(R.string.alarms_empty), Modifier.align(Alignment.Center))
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(alarms!!, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onClick = { onEdit(alarm.id) },
                            onActiveChange = { active -> scope.launch { book.setActive(alarm.id, active) } },
                        )
                    }
                }
            }
        }
        FloatingActionButton(onClick = onCreate, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(painterResource(R.drawable.ic_add), stringResource(R.string.alarm_new))
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    onClick: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
) {
    val time = alarm.time.label()
    val switchLabel = stringResource(R.string.alarm_switch, time)
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(time, style = MaterialTheme.typography.displaySmall)
                Text(
                    listOf(alarm.name, daysSummary(alarm.days)).filter { it.isNotEmpty() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                )
                alarm.challenges.forEach {
                    Text(challengeSummary(it), style = MaterialTheme.typography.bodySmall)
                }
            }
            Switch(
                checked = alarm.active,
                onCheckedChange = onActiveChange,
                modifier = Modifier.semantics { contentDescription = switchLabel },
            )
        }
    }
}

internal fun LocalTime.label() = "%02d:%02d".format(hour, minute)

@get:StringRes
internal val DayOfWeek.label
    get() =
        when (this) {
            DayOfWeek.MONDAY -> R.string.day_monday
            DayOfWeek.TUESDAY -> R.string.day_tuesday
            DayOfWeek.WEDNESDAY -> R.string.day_wednesday
            DayOfWeek.THURSDAY -> R.string.day_thursday
            DayOfWeek.FRIDAY -> R.string.day_friday
            DayOfWeek.SATURDAY -> R.string.day_saturday
            DayOfWeek.SUNDAY -> R.string.day_sunday
        }

@Composable
private fun daysSummary(days: Set<DayOfWeek>): String =
    when (days.size) {
        0 -> stringResource(R.string.alarm_once)
        DayOfWeek.entries.size -> stringResource(R.string.alarm_every_day)
        else -> days.sorted().map { stringResource(it.label) }.joinToString(", ")
    }
