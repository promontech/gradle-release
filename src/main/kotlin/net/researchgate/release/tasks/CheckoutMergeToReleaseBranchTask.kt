package net.researchgate.release.tasks

import net.researchgate.release.BaseScmAdapter
import net.researchgate.release.ReleaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Check that updates to the project are not needed
 */
open class CheckoutMergeToReleaseBranchTask : DefaultTask() {
    private var scmAdapter: BaseScmAdapter? = null
    var extension: ReleaseExtension = project.extensions.getByType(ReleaseExtension::class.java)
    private val log: Logger = project.logger ?: LoggerFactory.getLogger(this::class.java)

    @TaskAction
    fun task() {
        if (extension.pushReleaseVersionBranch && !extension.failOnCommitNeeded) {
            log.warn("""/!\Warning/!\""")
            log.warn("It is strongly discouraged to set failOnCommitNeeded to false with pushReleaseVersionBranch is enabled.")
            log.warn("Merging with an uncleaned working directory will lead to unexpected results.")
        }

        scmAdapter = project.tasks.getByPath("createScmAdapter").property("scmAdapter") as BaseScmAdapter
        scmAdapter?.checkoutMergeToReleaseBranch()
                ?: throw GradleException("No supported SCM Adapter could be found.")
    }
}
