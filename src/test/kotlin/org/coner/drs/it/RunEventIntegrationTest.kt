package org.coner.drs.it

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.coner.drs.domain.entity.Event
import org.coner.drs.domain.entity.Run
import org.coner.drs.io.gateway.EventGateway
import org.coner.drs.it.util.FilesystemFixture
import org.coner.drs.it.util.IntegrationTestApp
import org.coner.drs.test.extension.*
import org.coner.drs.test.extension.App
import org.coner.drs.test.fixture.integration.crispyfish.classdefinition.Thscc2019V0Classes
import org.coner.drs.test.fixture.integration.crispyfish.event.Thscc2019Points1
import org.coner.drs.test.page.AddNextDriverPage
import org.coner.drs.test.page.RunEventPage
import org.coner.drs.test.page.RunEventTablePage
import org.coner.drs.test.page.fast.FastChooseEventPage
import org.coner.drs.test.page.fast.FastStartPage
import org.coner.drs.test.page.real.RealAlterDriverSequencePage
import org.coner.drs.test.page.real.RealRunEventPage
import org.coner.drs.test.page.real.RealRunEventTablePage
import org.coner.drs.ui.chooseevent.ChooseEventModel
import org.coner.drs.ui.chooseevent.ChooseEventTableView
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.testfx.api.FxRobot
import tornadofx.*
import java.nio.file.Path
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@ExtendWith(TornadoFxAppExtension::class)
class RunEventIntegrationTest {

    @TempDir
    lateinit var tempDir: Path

    @App
    lateinit var app: tornadofx.App
    lateinit var folders: FilesystemFixture
    lateinit var event: Event
    lateinit var page: RunEventPage
    lateinit var addNextDriverPage: AddNextDriverPage
    lateinit var tablePage: RunEventTablePage

    @Init
    fun init() {
        folders = FilesystemFixture(tempDir)
    }

    @SetupApp
    fun setupApp() = IntegrationTestApp(folders.appConfigBasePath)

    @Start
    fun start(robot: FxRobot) {
        println(folders.root)
        val startPage = FastStartPage(find(app.scope), robot)
        startPage.setRawSheetDatabase(folders.rawSheetDatabase)
        startPage.setCrispyFishDatabase(folders.crispyFishDatabase.toFile())
        startPage.clickStartButton()
        val classDefinitionFile = Thscc2019V0Classes.produceClassDefinitionFile(folders.crispyFishDatabase.toFile())
        val eventControlFile = Thscc2019Points1.produceEventControlFile(folders.crispyFishDatabase.toFile(), classDefinitionFile)
        val event = Event(
                date = LocalDate.parse("2019-03-03"),
                name = "RunEventIntegrationTest",
                crispyFishMetadata = Event.CrispyFishMetadata(
                        classDefinitionFile = classDefinitionFile.file,
                        eventControlFile = eventControlFile.file
                )
        )
        val latch = CountDownLatch(2)
        val fastChooseEventPage = FastChooseEventPage(robot)
        find<ChooseEventModel>(app.scope).dockedProperty.onChangeOnce { runLater { latch.countDown() } }
        find<ChooseEventTableView>(app.scope).root.items.onChange { runLater { latch.countDown() } }
        FX.runAndWait { find<EventGateway>(app.scope).save(event) }
        latch.await(1, TimeUnit.SECONDS)
        fastChooseEventPage.selectEvent { it.id == event.id }
        fastChooseEventPage.clickStartButton()
        this.event = find<EventGateway>(app.scope).list().first()
        this.page = RealRunEventPage(robot)
        this.addNextDriverPage = page.toAddNextDriverPage()
        this.tablePage = RealRunEventTablePage(robot)
    }

    @Test
    fun itShouldDisplayEvent() {
        assertThat(page.root().text).isEqualTo(event.name)
    }

    @Test
    fun itShouldAddNextDriver() {
        Assumptions.assumeThat(tablePage.runsTable().items).hasSize(0)

        addNextDriverPage.writeInNumbersField("1 HS")
        addNextDriverPage.doAddSelectedRegistration()

        assertThat(tablePage.runsTable()).all {
            this.prop("items") { it.items }.all {
                hasSize(1)
                index(0).all {
                    prop(Run::registrationNumbers).isEqualTo("1 HS")
                }
            }
        }
    }

    @Test
    fun itShouldInsertDriverIntoSequence(robot: FxRobot) {
        val runsTableItems = tablePage.runsTable().items
        Assumptions.assumeThat(runsTableItems).hasSize(0)
        addNextDriverPage.writeInNumbersField("1 HS")
        addNextDriverPage.doAddSelectedRegistration()
        val alterDriverSequencePage = RealAlterDriverSequencePage(robot)
        val specifyDriverSequenceAlterationPage = alterDriverSequencePage.toSpecifyDriverSequenceAlterationPage()

        tablePage.clickInsertDriverIntoSequence(1)
        specifyDriverSequenceAlterationPage.writeInNumbersField("3 SSC")
        alterDriverSequencePage.clickOkButton()

        Assertions.assertThat(runsTableItems).hasSize(2)
        Assertions.assertThat(runsTableItems[0])
                .hasFieldOrPropertyWithValue("sequence", 1)
                .hasFieldOrPropertyWithValue("registrationNumbers", "3 SSC")
        Assertions.assertThat(runsTableItems[1])
                .hasFieldOrPropertyWithValue("sequence", 2)
                .hasFieldOrPropertyWithValue("registrationNumbers", "1 HS")
    }

    @Test
    fun itShouldIncrementAddNextDriverSequenceWhenInsertDriverIntoSequence(robot: FxRobot) {
        arrayOf("1 HS", "9 SM").forEach {
            addNextDriverPage.writeInNumbersField(it)
            addNextDriverPage.doAddSelectedRegistration()
        }
        val alterDriverSequencePage = RealAlterDriverSequencePage(robot)
        val specifyDriverSequenceAlterationPage = alterDriverSequencePage.toSpecifyDriverSequenceAlterationPage()
        val runsTableItems = tablePage.runsTable().items

        tablePage.clickInsertDriverIntoSequence(2)
        specifyDriverSequenceAlterationPage.writeInNumbersField("33 SM")
        alterDriverSequencePage.clickOkButton()

        Assertions.assertThat(addNextDriverPage.sequenceField().text).isEqualTo("4")
        Assertions.assertThat(runsTableItems).hasSize(3)
        Assertions.assertThat(runsTableItems[0].registrationNumbers).isEqualTo("1 HS")
        Assertions.assertThat(runsTableItems[1].registrationNumbers).isEqualTo("33 SM")
        Assertions.assertThat(runsTableItems[2].registrationNumbers).isEqualTo("9 SM")

        addNextDriverPage.writeInNumbersField("40 GS")
        addNextDriverPage.doAddSelectedRegistration()

        Assertions.assertThat(addNextDriverPage.sequenceField().text).isEqualTo("5")
        Assertions.assertThat(runsTableItems[3].registrationNumbers).isEqualTo("40 GS")
    }
}