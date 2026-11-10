package io.github.guilhermedkdk.wakeguard.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.guilhermedkdk.wakeguard.alarm.AlarmBook
import io.github.guilhermedkdk.wakeguard.alarm.AlarmDatabase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class WakeGuardAppTest {
    @get:Rule
    val compose = createComposeRule()

    private val database =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AlarmDatabase::class.java)
            .build()

    @Before
    fun open() = compose.setContent { WakeGuardApp(AlarmBook(database)) }

    @After
    fun close() = database.close()

    private fun waitFor(text: String) = compose.waitUntilAtLeastOneExists(hasText(text), 5_000)

    private fun createDefaultAlarm() {
        compose.onNodeWithText("Alarmes").performClick()
        compose.onNodeWithContentDescription("Novo alarme").performClick()
        compose.onNodeWithText("Salvar").performClick()
        waitFor("07:00")
    }

    @Test
    fun `opens on the home tab`() {
        compose.onNodeWithText("Início").assertIsSelected()
        compose.onNodeWithText("Nenhum alarme por enquanto.").assertIsDisplayed()
    }

    @Test
    fun `alarms tab starts empty`() {
        compose.onNodeWithText("Alarmes").performClick()

        compose.onNodeWithText("Alarmes").assertIsSelected()
        waitFor("Você ainda não criou nenhum alarme.")
    }

    @Test
    fun `new alarm shows up in the list with its days and challenges`() {
        createDefaultAlarm()

        compose.onNodeWithText("Uma vez").assertIsDisplayed()
        compose.onNodeWithText("Matemática · Médio · 3 contas").assertIsDisplayed()
    }

    @Test
    fun `alarm can be named and given weekdays`() {
        compose.onNodeWithText("Alarmes").performClick()
        compose.onNodeWithContentDescription("Novo alarme").performClick()
        compose.onNodeWithText("Seg").performScrollTo().performClick()
        compose.onNodeWithText("Qua").performScrollTo().performClick()
        compose.onNodeWithText("Nome").performScrollTo().performTextInput("Academia")
        compose.onNodeWithText("Salvar").performClick()

        waitFor("Academia · Seg, Qua")
    }

    @Test
    fun `alarm can be switched off from the list`() {
        createDefaultAlarm()

        compose.onNode(isToggleable()).performClick()

        compose.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun `editing an alarm adds a challenge to it`() {
        createDefaultAlarm()

        compose.onNodeWithText("07:00").performClick()
        compose.onNodeWithText("Adicionar desafio").performScrollTo().performClick()
        compose.onNodeWithText("Sacudir").performClick()
        compose.onNodeWithText("Confirmar").performClick()
        compose.onNodeWithText("Salvar").performClick()

        waitFor("Sacudir · Médio · 30 sacudidas")
        compose.onNodeWithText("Matemática · Médio · 3 contas").assertIsDisplayed()
    }

    @Test
    fun `alarm is deleted after confirming`() {
        createDefaultAlarm()

        compose.onNodeWithText("07:00").performClick()
        compose.onNodeWithText("Excluir alarme").performScrollTo().performClick()
        compose.onNodeWithText("Excluir").performClick()

        waitFor("Você ainda não criou nenhum alarme.")
    }
}
