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

import javafx.scene.layout.Region
import org.coner.drs.domain.entity.RunEvent
import tornadofx.*

class RunEventFragment : Fragment("Run Event") {
    val event: RunEvent by param()
    val subscriber: Boolean by param()
    val eventScope = Scope()

    val model: RunEventModel by inject(eventScope)
    val controller: RunEventController by inject(eventScope)

    init {
        model.event = event
        model.subscriber = subscriber
        controller.init()
    }

    override val root = borderpane {
        id = "run-event"
        top {
            add(find<RunEventTopView>(eventScope))
        }
        left {
            add(find<AddNextDriverView>(eventScope))
        }
        center {
            add(find<RunEventTableView>(eventScope))
        }
        right { add(find<RunEventRightDrawerView>(eventScope)) }
    }

    override fun onDock() {
        super.onDock()
        controller.docked()
    }

    override fun onUndock() {
        super.onUndock()
        controller.undocked()
    }


}