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

package org.coner.drs.domain.mapper

import org.coner.drs.domain.entity.Event
import org.coner.drs.domain.entity.Registration
import org.coner.drs.domain.entity.Run
import org.coner.drs.domain.payload.InsertDriverIntoSequenceResult
import org.coner.drs.io.db.entity.RunDbEntity
import org.coner.drs.ui.alterdriversequence.PreviewAlteredDriverSequenceResult
import tornadofx.*
import kotlin.streams.toList

object RunMapper {
    fun toUiEntity(event: Event, dbEntity: RunDbEntity?) = if (dbEntity != null) Run(
            id = dbEntity.id,
            event = event,
            sequence = dbEntity.sequence,
            registration = if (dbEntity.handicap.isNotBlank() && dbEntity.number.isNotBlank())
                Registration(
                    category = dbEntity.category,
                    handicap = dbEntity.handicap,
                    number = dbEntity.number
                )
            else
                null,
            rawTime = dbEntity.rawTime,
            cones = dbEntity.cones,
            didNotFinish = dbEntity.didNotFinish,
            disqualified = dbEntity.disqualified,
            rerun = dbEntity.rerun
    ) else null

    fun toDbEntity(uiRun: Run) = RunDbEntity(
            id = uiRun.id,
            eventId = uiRun.event.id,
            sequence = uiRun.sequence,
            category = uiRun.registration?.category ?: "",
            handicap = uiRun.registration?.handicap ?: "",
            number = uiRun.registration?.number ?: "",
            rawTime = uiRun.rawTime,
            cones = uiRun.cones,
            didNotFinish = uiRun.didNotFinish,
            disqualified = uiRun.disqualified,
            rerun = uiRun.rerun
    )

    fun copy(uiRun: Run) = Run(
            id = uiRun.id,
            event = uiRun.event,
            sequence = uiRun.sequence,
            registration = uiRun.registration,
            rawTime = uiRun.rawTime,
            cones = uiRun.cones,
            didNotFinish = uiRun.didNotFinish,
            disqualified = uiRun.disqualified,
            rerun = uiRun.rerun
    )

    fun toPreviewAlteredDriverSequenceResult(result: InsertDriverIntoSequenceResult): PreviewAlteredDriverSequenceResult {
        val runs = result.runs.parallelStream()
                .map { run ->
                    val status = when {
                        result.insertRunId == run.id -> PreviewAlteredDriverSequenceResult.Status.INSERTED
                        result.shiftRunIds.contains(run.id) -> PreviewAlteredDriverSequenceResult.Status.SHIFTED
                        else -> PreviewAlteredDriverSequenceResult.Status.SAME
                    }
                    PreviewAlteredDriverSequenceResult.Run(
                            run = run,
                            status = status
                    )
                }
                .toList()
                .toObservable()
        val insertedRun = runs.parallelStream()
                .filter { it.id == result.insertRunId }
                .findFirst()
                .get()
        return PreviewAlteredDriverSequenceResult(runs, insertedRun)
    }
}