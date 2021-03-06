/*
 * Coner Digital Raw Sheets - reduce the drag of working autocross raw sheets
 * Copyright (C) 2018-2020 Carlton Whitehead
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.coner.drs.ui.runevent

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import me.carltonwhitehead.tornadofx.junit5.Init
import me.carltonwhitehead.tornadofx.junit5.SetupApp
import me.carltonwhitehead.tornadofx.junit5.Start
import me.carltonwhitehead.tornadofx.junit5.TornadoFxAppExtension
import org.awaitility.Awaitility
import org.coner.drs.di.numberFormatModule
import org.coner.drs.domain.entity.Run
import org.coner.drs.domain.payload.InsertDriverIntoSequenceResult
import org.coner.drs.test.TestApp
import org.coner.drs.test.fixture.domain.entity.RunEvents
import org.coner.drs.test.page.fast.FastRunEventTablePage
import org.coner.drs.test.page.real.RealRunEventTablePage
import org.coner.drs.ui.alterdriversequence.AlterDriverSequenceController
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rewedigital.katana.Component
import org.testfx.api.FxRobot
import org.testfx.assertions.api.Assertions
import tornadofx.*

@ExtendWith(TornadoFxAppExtension::class, MockKExtension::class)
class RunEventTableViewTest {

    private lateinit var app: App
    private lateinit var view: RunEventTableView
    private lateinit var model: RunEventTableModel
    private lateinit var controller: RunEventTableController

    private lateinit var realPage: RealRunEventTablePage
    private lateinit var fastPage: FastRunEventTablePage

    @SetupApp
    fun setupApp() = TestApp(Component(numberFormatModule)).apply {
        app = this
    }

    @Start
    fun start(robot: FxRobot) {
        app.run {
            find<RunEventModel>(scope).apply {
                event = RunEvents.basic()
            }
            controller = find(scope)
            model = find(scope)
            view = find(scope)
        }
        FX.runAndWait { view.openWindow() }
    }

    @BeforeEach
    fun beforeEach(robot: FxRobot) {
        realPage = RealRunEventTablePage(robot)
        fastPage = FastRunEventTablePage(robot)
    }

    @Test
    fun `When user inserts driver into sequence it should select it`() {
        val event = find<RunEventModel>().event
        FX.runAndWait {
            event.runs += Run(
                    event = event,
                    sequence = 1,
                    registration = event.registrations[0]
            )
        }
        val alterDriverSequenceController: AlterDriverSequenceController = mockk(relaxed = true)
        val insertRun = Run(
                event = event,
                sequence = 1,
                registration = event.registrations[1]
        )
        every {
            alterDriverSequenceController.showAlterDriverSequenceAndWait(event.runs[0].sequence, event)
        }.answers {
            val runs = listOf(insertRun, event.runs[0])
            event.runs[0].sequence = 2
            event.runs.setAll(runs)
            InsertDriverIntoSequenceResult(
                    runs = runs,
                    insertRunId = insertRun.id,
                    shiftRunIds = hashSetOf(event.runs[0].id)
            )
        }
        setInScope(alterDriverSequenceController)
        val runsTable = fastPage.runsTable()
        val sequence = event.runs[0].sequence
        fastPage.selectSequence(sequence)

        realPage.keyboardShortcutInsertDriverIntoSequence(sequence)

        Awaitility.await().untilAsserted {
            Assertions.assertThat(runsTable.selectionModel.selectedItem).isSameAs(insertRun)
        }
    }
}