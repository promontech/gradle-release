/*
 * This file is part of the gradle-release plugin.
 *
 * (c) Eric Berry
 * (c) ResearchGate GmbH
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package net.researchgate.release

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File

class GitAdapter(project: Project, attributes: MutableMap<String, Any>) : BaseScmAdapter(project, attributes) {


//    companion object {
        private val LINE: String = "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"

        private val UNCOMMITTED: String = "uncommitted"
        private val UNVERSIONED: String = "unversioned"
        private val AHEAD: String = "ahead"
        private val BEHIND: String = "behind"

        private var workingBranch: String
        private var pushReleaseVersionBranch: Boolean? = null

        private lateinit var workingDirectory: File
//    }

    init {
        workingBranch = getBranch()
        pushReleaseVersionBranch = extension.pushReleaseVersionBranch
    }

    inner class GitConfig {
        var requireBranch = "master"
        var pushToRemote = "origin" // needs to be def as can be boolean or string
        var pushOptions = emptyList<String>()
        var signTag = false

        /** @deprecated Remove in version 3.0  */
        @Deprecated("You are setting the deprecated and unused option pushToCurrentBranch. You can safely remove it. The deprecated option will be removed in 3.0")
        var pushToCurrentBranch = false
        var pushToBranchPrefix: String = ""
        var commitVersionFileOnly = false

        fun setProperty(name: String, value: Any) {
            if (name == "pushToCurrentBranch") {
                project.logger.warn("You are setting the deprecated and unused option '$name'. You can safely remove it. The deprecated option will be removed in 3.0")
            }

//            metaClass.setProperty(this, name, value) // TODO tyler examine effect of this
        }
    }


    override fun createNewConfig(): Any {
        return GitConfig()
    }

    override fun isSupported(directory: File): Boolean {
        if (!directory.list().any { it == ".git" }) {
            return if (directory.parentFile != null) isSupported(directory.parentFile) else false
        }

        workingDirectory = directory
        return true
    }

    override fun init() {
        if (workingBranch.matches(extension.git.requireBranch.toRegex()).not()) {
            throw GradleException("Current Git branch is \"$workingBranch\" and not \"${extension.git.requireBranch}\".")
        }
    }

    override fun checkCommitNeeded() {
        val status = gitStatus()

        status[UNVERSIONED]?.isNotEmpty().run {
            warnOrThrow(extension.failOnUnversionedFiles,
                    listOf<Any?>("You have unversioned files:", LINE, status[UNVERSIONED]?.joinToString(), LINE).joinToString(separator = "\n"))
        }
        status[UNCOMMITTED]?.isNotEmpty().run {
            warnOrThrow(extension.failOnCommitNeeded,
                    listOf("You have uncommitted files:", LINE, status[UNCOMMITTED]?.joinToString(), LINE).joinToString(separator = "\n"))
        }
    }

    override fun checkUpdateNeeded() {
        exec(mutableMapOf("directory" to workingDirectory, "errorPatterns" to listOf("error: ", "fatal: ")), listOf("git", "remote", "update"))

        val status = gitRemoteStatus()

        status[AHEAD]?.run { warnOrThrow(extension.failOnPublishNeeded, "You have ${status[AHEAD]} local change(s) to push.") }
        status[BEHIND]?.run { warnOrThrow(extension.failOnUpdateNeeded, "You have ${status[BEHIND]} remote change(s) to pull.") }
    }

    override fun createReleaseTag(message: String) {
        val tagName = tagName()
        val params = mutableListOf("git", "tag", "-a", tagName, "-m", message)
        if (extension.git.signTag) {
            params.add("-s")
        }
        exec(params = params, directory = workingDirectory, errorMessage = "Duplicate tag [$tagName]", errorPatterns = listOf("already exists"))

        if (shouldPush()) {
            val params1 = mutableListOf<String>("git", "push", "--porcelain", extension.git.pushToRemote, tagName)
            params1.addAll(extension.git.pushOptions)
            exec(params = params1,
                    directory = workingDirectory,
                    errorMessage = "Failed to push tag [$tagName] to remote",
                    errorPatterns = listOf("[rejected]", "error: ", "fatal: "))
        }
    }

    private fun exec(directory: File? = null,
                     errorMessage: String? = null,
                     errorPatterns: List<String>? = null,
                     failOnStderr: Boolean? = null,
                     params: List<String>): String {
        val map: MutableMap<Any, Any> = mutableMapOf()
        directory?.let { map.put("directory", directory) }
        errorMessage?.let { map.put("errorMessage", errorMessage) }
        errorPatterns?.let { map.put("errorPatterns", errorPatterns) }
        failOnStderr?.let { map.put("failOnStderr", failOnStderr) }
        return exec(map, params)
    }

    override fun commit(message: String) {
        val command: MutableList<String> = mutableListOf("git", "commit", "-m", message)
        if (extension.git.commitVersionFileOnly) {
            project?.file(extension.versionPropertyFile)?.absolutePath?.let { command.add(it) }
        } else {
            command.add("-a")
        }

        exec(params = command, directory = workingDirectory, errorPatterns = listOf("error: ", "fatal: "))

        if (shouldPush()) {
            var branch = getBranch()
            if (extension.git.pushToBranchPrefix.isNotEmpty()) {
                branch = "HEAD:${extension.git.pushToBranchPrefix}$branch"
            }
            val params = mutableListOf("git", "push", "--porcelain", extension.git.pushToRemote, branch)
            params.addAll(extension.git.pushOptions)
            exec(params = params, directory = workingDirectory, errorMessage = "Failed to push to remote", errorPatterns = listOf("[rejected]", "error: ", "fatal: "))
        }
    }

    @Override
    override fun getBranch(): String {
        return exec(mutableMapOf(), listOf("git", "rev-parse", "--abbrev-ref", "HEAD")).replace("\\s", "")
    }

    override fun add(file: File) {
        exec(params = listOf("git", "add", file.path), directory = workingDirectory, errorMessage = "Error adding file ${file.name}", errorPatterns = listOf("error: ", "fatal: "))
    }

    override fun revert() {
        // Revert changes on gradle.properties
        exec(params = listOf("git", "checkout", findPropertiesFile().name), directory = workingDirectory, errorMessage = "Error reverting changes made by the release plugin.")
    }

    override fun checkoutMergeToReleaseBranch() {
        checkout(getReleaseBranch())
        merge(workingBranch)
    }

    override fun checkoutMergeFromReleaseBranch() {
        checkout(workingBranch)
        merge(getReleaseBranch())
    }

    override fun checkoutReleaseBranch() {
        checkout(getReleaseBranch())
    }

    private fun getReleaseBranch(): String {
        return if (pushReleaseVersionBranch == true) "release/${getReleaseVersion().replace("-SNAPSHOT", "")}" else workingBranch
    }

    private fun checkout(branch: String) {
        exec(params = listOf("git", "fetch"), directory = workingDirectory, errorPatterns = listOf("error: ", "fatal: "))
        exec(params = listOf("git", "branch", branch), directory = workingDirectory, failOnStderr = false)
        exec(params = listOf("git", "checkout", branch), directory = workingDirectory, errorPatterns = listOf("error: ", "fatal: "))
    }

    private fun merge(fromBranch: String) {
        exec(params = listOf("git", "merge", "--no-ff", "--no-commit", fromBranch), directory = workingDirectory, errorPatterns = listOf("error: ", "fatal: ", "CONFLICT"))
    }

    private fun shouldPush(): Boolean {
        var shouldPush = false
        val exec = exec(params = listOf("git", "remote"), directory = workingDirectory)
        exec.split("\n").forEach { line ->
            val regex = Regex("""^\s*(.*)\s*${'$'}""")
            if (regex.matches(line)) {
                val match = regex.matchEntire(line)
                if (match?.groupValues?.get(1) == extension.git.pushToRemote) {
                    shouldPush = true
                }
            }
        }
        if (!shouldPush && extension.git.pushToRemote != "origin") {
            throw GradleException("Could not push to remote ${extension.git.pushToRemote} as repository has no such remote")
        }

        return shouldPush
    }

    private fun gitStatus(): Map<String, List<String>> {
        return exec(mutableMapOf("directory" to workingDirectory), listOf("git", "status", "--porcelain")).split("\n").groupBy {
            if (it.matches(Regex("""^\s*\?{2}.*"""))) {
                UNVERSIONED
            } else {
                UNCOMMITTED
            }
        }
    }

    private fun gitRemoteStatus(): Map<String, Int> {
        val branchStatus = exec(mutableMapOf("directory" to workingDirectory), listOf("git", "status", "--porcelain", "-b")).split("\n")[0]
        val aheadRegex = Regex(""".*ahead (\d+).*""")
        val aheadMatcher = aheadRegex.find(branchStatus)

        val behindRegex = Regex(""".*behind (\d+).*""")
        val behindMatcher = behindRegex.matchEntire(branchStatus)

        val remoteStatus = mutableMapOf<String, Int>()

        aheadMatcher?.let { remoteStatus[AHEAD] = it.groupValues.first() as Int }
        behindMatcher?.let { remoteStatus[BEHIND] = it.groupValues.first() as Int }
        return remoteStatus
    }
}
