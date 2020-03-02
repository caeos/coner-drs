package org.coner.drs.ui.home

import org.coner.drs.di.katanaAppComponent
import org.coner.drs.di.katanaScopes
import org.coner.drs.io.DrsIoController
import org.coner.drs.ui.chooseevent.ChooseEventView
import tornadofx.*
import java.io.File
import java.nio.file.Path

class HomeView : View() {

    private val model: HomeModel by inject()

    private val drsIo: DrsIoController by inject()

    override val root = stackpane()

    fun prepareDrsIo(pathToDrsDb: Path, pathToCfDb: File) {
        drsIo.open(
                pathToDrsDatabase = pathToDrsDb,
                pathToCrispyFishDatabase = pathToCfDb
        )
    }

    override fun onDock() {
        super.onDock()
        root.add(find<ChooseEventView>())
    }

    override fun onUndock() {
        super.onUndock()
        root.replaceChildren()
    }

    companion object {
        fun create(
                component: Component,
                pathToDigitalRawSheetsDatabase: Path
        ): HomeView {
            val fxScope = Scope()
            component.katanaScopes.home = HomeKatanaScope(
                    appComponent = component.katanaAppComponent,
                    pathToDigitalRawSheetsDatabase = pathToDigitalRawSheetsDatabase
            )
            return component.find(
                    params = mapOf(
                            HomeView::scope to fxScope
                    ),
                    scope = fxScope,
                    componentType = HomeView::class.java
            )
        }
    }
}
