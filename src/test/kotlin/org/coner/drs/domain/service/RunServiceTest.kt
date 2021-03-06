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

package org.coner.drs.domain.service

import assertk.all
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isSameAs
import assertk.assertions.prop
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assumptions
import org.coner.drs.domain.entity.Run
import org.coner.drs.domain.payload.InsertDriverIntoSequenceRequest
import org.coner.drs.io.gateway.RunGateway
import org.coner.drs.test.TornadoFxScopeExtension
import org.coner.drs.test.fixture.domain.entity.RunEvents
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tornadofx.*
import java.math.BigDecimal

@ExtendWith(TornadoFxScopeExtension::class)
class RunServiceTest {

    private lateinit var service: RunService

    private lateinit var gateway: RunGateway

    @BeforeEach
    fun before(scope: Scope) {
        scope.set(mockk<RunGateway>(relaxed = true))
        service = find(scope)
        gateway = find(scope)
    }

    @Test
    fun `it should find blank run with sequence 1 when no runs yet exist`() {
        val runEvent = RunEvents.basic()
        Assumptions.assumeThat(runEvent.runsBySequence).isEmpty()

        val actual = service.findRunForNextTime(runEvent).blockingGet()

        assertk.assertThat(actual).all {
            prop(Run::sequence).isEqualTo(1)
            prop(Run::registration).isNull()
        }
    }

    @Test
    fun `it should find first run when only runs without times exist`() {
        val runEvent = RunEvents.basic()
        Assumptions.assumeThat(runEvent.runsBySequence).isEmpty()
        runEvent.runs += Run(
                sequence = 1,
                registration = runEvent.registrations[0],
                event = runEvent,
                rawTime = null
        )

        val actual = service.findRunForNextTime(runEvent).blockingGet()

        assertk.assertThat(actual).all {
            prop(Run::sequence).isEqualTo(1)
            prop(Run::registration).isSameAs(runEvent.registrations[0])
        }
    }

    @Test
    fun `it should find second run when first run has time already`() {
        val runEvent = RunEvents.basic()
        Assumptions.assumeThat(runEvent.runsBySequence).isEmpty()
        runEvent.runs += Run(
                sequence = 1,
                registration = runEvent.registrations[0],
                event = runEvent,
                rawTime = BigDecimal.valueOf(123456, 3)
        )
        runEvent.runs += Run(
                sequence = 2,
                registration = runEvent.registrations[1],
                event = runEvent,
                rawTime = null
        )

        val actual = service.findRunForNextTime(runEvent).blockingGet()

        assertk.assertThat(actual).all {
            prop(Run::sequence).isEqualTo(2)
            prop(Run::registration).isSameAs(runEvent.registrations[1])
        }
    }

    @Test
    fun `it should make up a run when all runs in sequence have times already`() {
        val runEvent = RunEvents.basic()
        Assumptions.assumeThat(runEvent.runsBySequence).isEmpty()
        runEvent.runs += Run(
                sequence = 1,
                registration = runEvent.registrations[0],
                event = runEvent,
                rawTime = BigDecimal.valueOf(123456, 3)
        )

        val actual = service.findRunForNextTime(runEvent).blockingGet()

        assertk.assertThat(actual).all {
            prop(Run::sequence).isEqualTo(2)
            prop(Run::registration).isNull()
        }
    }

    @Test
    fun `it should insert driver into sequence before given sequence`() {
        val event = RunEvents.basic()
        val runs = listOf(
                Run(
                        event = event,
                        sequence = 1,
                        registration = event.registrations[0],
                        rawTime = BigDecimal.valueOf(1000, 3)
                ),
                Run(
                        event = event,
                        sequence = 2,
                        registration = event.registrations[2],
                        rawTime = BigDecimal.valueOf(2000, 3)
                )
        )
        val request = InsertDriverIntoSequenceRequest(
                event = event,
                runs = runs,
                sequence = 2,
                relative = InsertDriverIntoSequenceRequest.Relative.BEFORE,
                registration = event.registrations[1],
                dryRun = true
        )

        val actual = service.insertDriverIntoSequence(request).blockingGet()

        Assertions.assertThat(actual.runs).hasSize(3)
        Assertions.assertThat(actual.runs[0])
                .hasFieldOrPropertyWithValue("id", runs[0].id)
                .hasFieldOrPropertyWithValue("sequence", 1)
                .hasFieldOrPropertyWithValue("registration", event.registrations[0])
                .hasFieldOrPropertyWithValue("rawTime", runs[0].rawTime)
        Assertions.assertThat(actual.runs[1])
                .hasFieldOrPropertyWithValue("sequence", 2)
                .hasFieldOrPropertyWithValue("registration", event.registrations[1])
                .hasFieldOrPropertyWithValue("rawTime", runs[1].rawTime)
        Assertions.assertThat(actual.runs[2])
                .hasFieldOrPropertyWithValue("id", runs[1].id)
                .hasFieldOrPropertyWithValue("sequence", 3)
                .hasFieldOrPropertyWithValue("registration", event.registrations[2])
                .hasFieldOrPropertyWithValue("rawTime", null)
    }

    @Test
    fun `it should insert driver into sequence after given sequence`() {
        val event = RunEvents.basic()
        val runs = listOf(
                Run(
                        event = event,
                        sequence = 1,
                        registration = event.registrations[0],
                        rawTime = BigDecimal.valueOf(1000, 3)
                ),
                Run(
                        event = event,
                        sequence = 2,
                        registration = event.registrations[2],
                        rawTime = BigDecimal.valueOf(2000, 3)
                )
        )
        val request = InsertDriverIntoSequenceRequest(
                event = event,
                runs = runs,
                sequence = 1,
                relative = InsertDriverIntoSequenceRequest.Relative.AFTER,
                registration = event.registrations[1],
                dryRun = true
        )

        val actual = service.insertDriverIntoSequence(request).blockingGet()

        Assertions.assertThat(actual.runs).hasSize(3)
        Assertions.assertThat(actual.runs[0])
                .hasFieldOrPropertyWithValue("id", runs[0].id)
                .hasFieldOrPropertyWithValue("sequence", 1)
                .hasFieldOrPropertyWithValue("registration", event.registrations[0])
                .hasFieldOrPropertyWithValue("rawTime", runs[0].rawTime)
        Assertions.assertThat(actual.runs[1])
                .hasFieldOrPropertyWithValue("sequence", 2)
                .hasFieldOrPropertyWithValue("registration", event.registrations[1])
                .hasFieldOrPropertyWithValue("rawTime", runs[1].rawTime)
        Assertions.assertThat(actual.runs[2])
                .hasFieldOrPropertyWithValue("id", runs[1].id)
                .hasFieldOrPropertyWithValue("sequence", 3)
                .hasFieldOrPropertyWithValue("registration", event.registrations[2])
                .hasFieldOrPropertyWithValue("rawTime", null)
    }

    @Test
    fun `it should not save with gateway on insert driver into sequence dry run`() {
        val event = RunEvents.basic()
        val runs = listOf(
                Run(
                        event = event,
                        sequence = 1,
                        registration = event.registrations[1]
                )
        )
        val request = InsertDriverIntoSequenceRequest(
                event = event,
                runs = runs,
                sequence = 1,
                relative = InsertDriverIntoSequenceRequest.Relative.BEFORE,
                registration = event.registrations[0],
                dryRun = true
        )

        service.insertDriverIntoSequence(request).blockingGet()

        verify(exactly = 0) { gateway.save(any()) }
    }

    @Test
    fun `it should save with gateway on insert driver into sequence`() {
        val event = RunEvents.basic()
        val runs = listOf(
                Run(
                        event = event,
                        sequence = 1,
                        registration = event.registrations[1]
                )
        )
        val request = InsertDriverIntoSequenceRequest(
                event = event,
                runs = runs,
                sequence = 1,
                relative = InsertDriverIntoSequenceRequest.Relative.BEFORE,
                registration = event.registrations[0],
                dryRun = false
        )

        service.insertDriverIntoSequence(request).blockingGet()

        verify(exactly = 2) { gateway.save(any()) }
    }

    @Test
    fun `It should clear time from run`() {
        val event = RunEvents.basic()
        val run = Run(
                event = event,
                registration = event.registrations[0],
                rawTime = BigDecimal.valueOf(123456, 3)
        )
        Assumptions.assumeThat(run.rawTime).isNotNull()

        service.changeTime(run, null).blockingAwait()

        Assertions.assertThat(run)
                .hasFieldOrPropertyWithValue("rawTime", null)
        verify { gateway.save(run) }
    }
}