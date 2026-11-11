package io.github.guilhermedkdk.wakeguard.ringing

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.guilhermedkdk.wakeguard.alarm.Alarm
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeSpec
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class RingingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val alarm =
        Alarm(
            time = LocalTime.of(7, 0),
            name = "Academia",
            challenges = listOf(ChallengeSpec(ChallengeType.MATH), ChallengeSpec(ChallengeType.SHAKE)),
        )
    private var rehearsalsEnded = 0

    private fun show(state: RingingState) =
        compose.setContent { RingingScreen(state, {}, {}, onEndRehearsal = { rehearsalsEnded++ }) }

    @Test
    fun `shows the alarm name and which challenge it is on`() {
        show(RingingState(alarm, rehearsal = false, challengeIndex = 1))

        compose.onNodeWithText("Academia").assertIsDisplayed()
        compose.onNodeWithText("Desafio 2 de 2").assertIsDisplayed()
        compose.onNodeWithText("Sacudir · Médio · 30 sacudidas").assertIsDisplayed()
    }

    @Test
    fun `a real ringing has no way to be ended`() {
        show(RingingState(alarm, rehearsal = false, challengeIndex = 0))

        compose.onNodeWithText("Encerrar teste").assertDoesNotExist()
    }

    @Test
    fun `a rehearsal can be ended`() {
        show(RingingState(alarm, rehearsal = true, challengeIndex = 0))

        compose.onNodeWithText("Encerrar teste").performClick()

        assertEquals(1, rehearsalsEnded)
    }
}
