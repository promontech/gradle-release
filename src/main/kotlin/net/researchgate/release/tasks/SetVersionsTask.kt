package net.researchgate.release.tasks

import net.researchgate.release.BaseScmAdapter
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Check that commits are not needed 
 */
open class SetVersionsTask : DefaultTask() {
    private var scmAdapter: BaseScmAdapter? = null

    @TaskAction
    fun createScmAdapter() {
        scmAdapter = project.tasks.getByPath("createScmAdapter").property("scmAdapter") as BaseScmAdapter
        scmAdapter!!.checkCommitNeeded()
    }
}
