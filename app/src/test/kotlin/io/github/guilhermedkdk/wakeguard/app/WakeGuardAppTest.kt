package io.github.guilhermedkdk.wakeguard.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WakeGuardAppTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `opens on the home tab`() {
        compose.setContent { WakeGuardApp() }

        compose.onNodeWithText("Início").assertIsSelected()
        compose.onNodeWithText("Nenhum alarme por enquanto.").assertIsDisplayed()
    }

    @Test
    fun `switches to the tab that was tapped`() {
        compose.setContent { WakeGuardApp() }

        compose.onNodeWithText("Alarmes").performClick()

        compose.onNodeWithText("Alarmes").assertIsSelected()
        compose.onNodeWithText("Você ainda não criou nenhum alarme.").assertIsDisplayed()
    }
}
