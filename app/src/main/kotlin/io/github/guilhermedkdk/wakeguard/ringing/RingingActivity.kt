package io.github.guilhermedkdk.wakeguard.ringing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.guilhermedkdk.wakeguard.R
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeScreen
import io.github.guilhermedkdk.wakeguard.theme.WakeGuardTheme
import kotlinx.coroutines.delay
import java.time.LocalTime

// Draws the ringing the service publishes, over the lock screen. Holds no ringing state of its own.
internal class RingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        val controller = RingingController.of(this)
        setContent {
            // There is no way out of a ringing but its challenges.
            BackHandler {}
            val state by controller.state.collectAsStateWithLifecycle()
            LaunchedEffect(state == null) { if (state == null) finishAndRemoveTask() }
            WakeGuardTheme {
                state?.let {
                    RingingScreen(
                        state = it,
                        onChallengeCompleted = { controller.service?.challengeCompleted() },
                        onInteraction = { controller.service?.interacted() },
                        onEndRehearsal = { controller.service?.endRehearsal() },
                    )
                }
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, RingingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
internal fun RingingScreen(
    state: RingingState,
    onChallengeCompleted: () -> Unit,
    onInteraction: () -> Unit,
    onEndRehearsal: () -> Unit,
) {
    val time by produceState(LocalTime.now()) {
        while (true) {
            delay(1_000)
            value = LocalTime.now()
        }
    }
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("%02d:%02d".format(time.hour, time.minute), style = MaterialTheme.typography.displayLarge)
            if (state.alarm.name.isNotEmpty()) Text(state.alarm.name, style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.ringing_progress, state.challengeIndex + 1, state.alarm.challenges.size),
                style = MaterialTheme.typography.titleMedium,
            )
            // A new challenge starts from scratch, even when it has the same spec as the one before.
            key(state.challengeIndex) {
                ChallengeScreen(
                    state.challenge,
                    onChallengeCompleted,
                    onInteraction,
                    Modifier.weight(1f).fillMaxWidth(),
                )
            }
            if (state.rehearsal) {
                OutlinedButton(onClick = onEndRehearsal, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.ringing_end_rehearsal))
                }
            }
        }
    }
}
