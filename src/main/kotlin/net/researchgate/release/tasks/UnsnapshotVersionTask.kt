package net.researchgate.release.tasks

import net.researchgate.release.BaseScmAdapter
import net.researchgate.release.PropertiesFileHandler
import net.researchgate.release.ReleaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger

/**
 * Check that updates to the project are not needed
 */
open class UnsnapshotVersionTask : DefaultTask() {
    private var scmAdapter: BaseScmAdapter? = null
    var extension: ReleaseExtension = project.extensions.getByType(ReleaseExtension::class.java)
    private val log: Logger = project.logger
    private val propertiesFileHandler = PropertiesFileHandler(project, extension)

    @TaskAction
    fun task() {
        propertiesFileHandler.checkPropertiesFile()
        val version = project.version.toString()

//        if (version.contains("-SNAPSHOT")) {
//            propertiesFileHandler.attributes["usesSnapshot"] = true
//            version.replace("-SNAPSHOT", "")
//            updateVersionProperty(version)
//        }
//        scmAdapter.commit(extension.releaseVersionCommitMessage + " "$ { tagName() }".")

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
