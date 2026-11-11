package io.github.guilhermedkdk.wakeguard.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.guilhermedkdk.wakeguard.R

/**
 * Runs one challenge. Calls [onInteraction] whenever the person acts on it and [onCompleted] once, when
 * the whole amount is done.
 */
@Composable
fun ChallengeScreen(
    spec: ChallengeSpec,
    onCompleted: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Stand-in until each challenge type has its own screen: one tap interacts, the button completes.
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(challengeSummary(spec))
        Button(onClick = onInteraction) { Text(stringResource(R.string.challenge_placeholder_interact)) }
        Button(onClick = onCompleted) { Text(stringResource(R.string.challenge_placeholder_complete)) }
    }
}
