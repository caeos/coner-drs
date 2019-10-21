package org.coner.drs.test.page.real

import javafx.scene.Node
import javafx.scene.control.TableCell
import javafx.scene.control.TableView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import org.awaitility.Awaitility
import org.awaitility.Awaitility.await
import org.coner.drs.domain.entity.Run
import org.coner.drs.test.page.AlterDriverSequencePage
import org.coner.drs.test.page.RunEventTablePage
import org.testfx.api.FxRobot
import tornadofx.*

open class RealRunEventTablePage(private val robot: FxRobot) : RunEventTablePage {

    override fun root() = robot.lookup("#run-event-table")
            .query<Form>()

    override fun runsTable() = robot.from(root())
            .lookup("#runs-table")
            .query<TableView<Run>>()

    override fun tableCellForSequence(sequence: Int): TableCell<Run, Int> {
        return robot.from(runsTable())
                .lookup<TableCell<Run, Int>> { it.text == sequence.toString() }
                .query()
    }

    override fun selectSequence(sequence: Int) {
        val cell = tableCellForSequence(sequence)
        robot.clickOn(cell)
    }

    override fun keyboardShortcutInsertDriverIntoSequence(sequence: Int): AlterDriverSequencePage {
        check(runsTable().selectionModel.selectedItem?.sequence == sequence) {
            "Select run with sequence prior to use"
        }
        robot.press(KeyCode.CONTROL)
        robot.type(KeyCode.INSERT)
        robot.release(KeyCode.CONTROL)
        return RealAlterDriverSequencePage(robot)
    }

    override fun keyboardShortcutClearTime(sequence: Int) {
        val cell = tableCellForSequence(sequence)
        robot.clickOn(cell)
        robot.press(KeyCode.CONTROL)
        robot.type(KeyCode.C)
        robot.release(KeyCode.CONTROL)
    }

    override fun keyboardShortcutDeleteRun(sequence: Int) {
        val cell = tableCellForSequence(sequence)
        robot.clickOn(cell)
        robot.press(KeyCode.CONTROL)
        robot.type(KeyCode.DELETE)
        robot.release(KeyCode.CONTROL)
    }
}