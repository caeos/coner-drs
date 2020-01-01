package org.coner.drs.report

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import me.carltonwhitehead.tornadofx.junit5.SetupApp
import me.carltonwhitehead.tornadofx.junit5.TornadoFxAppExtension
import org.coner.drs.domain.entity.RunEvent
import org.coner.drs.io.DrsIoController
import org.coner.drs.io.gateway.EventGateway
import org.coner.drs.io.gateway.RegistrationGateway
import org.coner.drs.io.gateway.RunGateway
import org.coner.drs.test.fixture.FixtureUtil
import org.coner.drs.test.fixture.TestEventFixture
import org.coner.drs.util.NumberFormat
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import tornadofx.*
import java.awt.Desktop
import java.io.File
import java.util.*

@ExtendWith(TornadoFxAppExtension::class)
class J2HtmlAuditListReportTest {

    lateinit var app: App

    @SetupApp
    fun setupApp() = App().apply {
        app = this
    }

    @Test
    fun itShouldRender(@TempDir tempDir: File) {
        val fixture = TestEventFixture.Thscc2019Points9.factory(tempDir)
        val eventId = fixture.source.eventIds.single()
        val event = FixtureUtil.loadRunEvent(app, fixture, eventId)

        val actual = J2HtmlAuditListReport(event, NumberFormat.forRunTimes()).render()

//        Desktop.getDesktop()
//                .open(
//                        tempDir.resolve("itShouldRender.html").apply {
//                            writeText(actual)
//                        }
//                )
//
//        Thread.sleep(1000)

        AuditListReportTestUtil.assert(actual)
    }
}
