package net.researchgate.release

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Check that updates to the project are not needed
 */
open class CheckUpdateNeededTask : DefaultTask() {
    private var scmAdapter: BaseScmAdapter? = null

    @TaskAction
    fun task() {
        scmAdapter = project.tasks.getByPath("createScmAdapter").property("scmAdapter") as BaseScmAdapter
        scmAdapter!!.checkUpdateNeeded()
    }
}
