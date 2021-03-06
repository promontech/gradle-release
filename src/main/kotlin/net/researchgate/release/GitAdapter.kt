package net.researchgate.release/*
 * This file is part of the gradle-release plugin.
 *
 * (c) Eric Berry
 * (c) ResearchGate GmbH
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import net.researchgate.release.configs.GitConfig
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import kotlin.collections.set

class GitAdapter(project: Project, attributes: MutableMap<String, Any>) : BaseScmAdapter(project, attributes) {
    companion object {
        private const val LINE: String = "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"

        private const val UNCOMMITTED: String = "uncommitted"
        private const val UNVERSIONED: String = "unversioned"
        private const val AHEAD: String = "ahead"
        private const val BEHIND: String = "behind"

        private lateinit var workingBranch: String
        private var pushReleaseVersionBranch: Boolean? = null

        private lateinit var workingDirectory: File
    }

    private lateinit var gitConfig: GitConfig

    init {
        workingBranch = getBranch()
        pushReleaseVersionBranch = extension.pushReleaseVersionBranch
    }

    override fun isSupported(directory: File): Boolean {
        if (!directory.list().any { it == ".git" }) {
            return if (directory.parentFile != null) isSupported(directory.parentFile) else false
        }

        workingDirectory = directory
        return true
    }

    override fun init() {
        // not actually possible for this to be null unless someone messes up the order of construction
        gitConfig = extension.git
        if (workingBranch.matches(gitConfig.requireBranch.toRegex()).not()) {
            throw GradleException("Current Git branch is \"$workingBranch\" and not \"${gitConfig.requireBranch}\".")
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
        if (gitConfig.signTag) {
            params.add("-s")
        }
        exec(params = params, directory = workingDirectory, errorMessage = "Duplicate tag [$tagName]", errorPatterns = listOf("already exists"))

        if (shouldPush()) {
            val params1 = mutableListOf<String>("git", "push", "--porcelain", gitConfig.pushToRemote, tagName)
            params1.addAll(gitConfig.pushOptions)
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
        if (gitConfig.commitVersionFileOnly) {
            project.file(extension.versionPropertyFile).absolutePath?.let { command.add(it) }
        } else {
            command.add("-a")
        }

        exec(params = command, directory = workingDirectory, errorPatterns = listOf("error: ", "fatal: "))

        if (shouldPush()) {
            var branch = getBranch()
            if (gitConfig.pushToBranchPrefix.isNotEmpty()) {
                branch = "HEAD:${gitConfig.pushToBranchPrefix}$branch"
            }
            val params = mutableListOf("git", "push", "--porcelain", gitConfig.pushToRemote, branch)
            params.addAll(gitConfig.pushOptions)
            exec(params = params, directory = workingDirectory, errorMessage = "Failed to push to remote", errorPatterns = listOf("[rejected]", "error: ", "fatal: "))
        }
    }

    @Override
    override fun getBranch(): String {
        return exec(mutableMapOf(), listOf("git", "rev-parse", "--abbrev-ref", "HEAD")).replace("\\s", "").replace("\n", "")
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
                if (match?.groupValues?.get(1) == gitConfig.pushToRemote) {
                    shouldPush = true
                }
            }
        }
        if (!shouldPush && gitConfig.pushToRemote != "origin") {
            throw GradleException("Could not push to remote ${gitConfig.pushToRemote} as repository has no such remote")
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
