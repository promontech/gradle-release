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

import org.codehaus.groovy.runtime.DefaultGroovyMethods.grep
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.internal.impldep.org.bouncycastle.asn1.iana.IANAObjectIdentifiers.directory
import org.gradle.internal.impldep.org.bouncycastle.asn1.x500.style.RFC4519Style.name
import sun.jvm.hotspot.oops.CellTypeState.ref
import java.io.File

import java.util.regex.Matcher

class GitAdapterKotlin(project: Project?, attributes: MutableMap<String, Any>?) : BaseScmAdapter(project, attributes) {

    init {
        workingBranch = branch
        pushReleaseVersionBranch = extension.pushReleaseVersionBranch
    }

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

    inner class GitConfig {
        var requireBranch = "master"
        var pushToRemote = "origin" // needs to be def as can be boolean or string
        var pushOptions = emptyList()
        var signTag = false

        /** @deprecated Remove in version 3.0  */
        @Deprecated("You are setting the deprecated and unused option pushToCurrentBranch. You can safely remove it. The deprecated option will be removed in 3.0")
        var pushToCurrentBranch = false
        lateinit var pushToBranchPrefix: String
        var commitVersionFileOnly = false

        fun setProperty(name: String, value: Any) {
            if (name == "pushToCurrentBranch") {
                project.logger.warn("You are setting the deprecated and unused option '${name}'. You can safely remove it. The deprecated option will be removed in 3.0")
            }

            metaClass.setProperty(this, name, value)
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
        exec(mapOf("directory" to workingDirectory, "errorPatterns" to listOf("error: ", "fatal: ")), listOf("git", "remote", "update"))

        val status = gitRemoteStatus()

        if (status[AHEAD]) {
            warnOrThrow(extension.failOnPublishNeeded, "You have ${status[AHEAD]} local change(s) to push.")
        }

        if (status[BEHIND]) {
            warnOrThrow(extension.failOnUpdateNeeded, "You have ${status[BEHIND]} remote change(s) to pull.")
        }
    }

//    @Override
//    void createReleaseTag(String message) {
//        def tagName = tagName()
//        def params = ["git", "tag", "-a", tagName, "-m", message]
//        if (extension.git.signTag) {
//            params.add("-s")
//        }
//        exec(params, directory: workingDirectory, errorMessage: "Duplicate tag [$tagName]", errorPatterns: ["already exists"])
//        if (shouldPush()) {
//            exec(["git", "push", "--porcelain", extension.git.pushToRemote, tagName] + extension.git.pushOptions, directory: workingDirectory, errorMessage: "Failed to push tag [$tagName] to remote", errorPatterns: ["[rejected]", "error: ", "fatal: "])
//        }
//    }
//
//    @Override
//    void commit(String message) {
//        List<String> command = ["git", "commit", "-m", message]
//        if (extension.git.commitVersionFileOnly) {
//            command << project.file(extension.versionPropertyFile)
//        } else {
//            command << "-a"
//        }
//
//        exec(command, directory: workingDirectory, errorPatterns: ["error: ", "fatal: "])
//
//        if (shouldPush()) {
//            def branch = gitCurrentBranch()
//            if (extension.git.pushToBranchPrefix) {
//                branch = "HEAD:${extension.git.pushToBranchPrefix}${branch}"
//            }
//            exec(["git", "push", "--porcelain", extension.git.pushToRemote, branch] + extension.git.pushOptions, directory: workingDirectory, errorMessage: "Failed to push to remote", errorPatterns: ["[rejected]", "error: ", "fatal: "])
//        }
//    }

    @Override
    override fun getBranch(): String {
        return exec(listOf("git", "rev-parse", "--abbrev-ref", "HEAD")).replace("\\s", "")
    }

//    @Override
//    void add(File file) {
//        exec(["git", "add", file.path], directory: workingDirectory, errorMessage: "Error adding file ${file.name}", errorPatterns: ["error: ", "fatal: "])
//    }
//
//    @Override
//    void revert() {
//        // Revert changes on gradle.properties
//        exec(["git", "checkout", findPropertiesFile().name], directory: workingDirectory, errorMessage: "Error reverting changes made by the release plugin.")
//    }
//
//    @Override
//    void checkoutMergeToReleaseBranch() {
//        checkout(getReleaseBranch())
//        merge(workingBranch)
//    }
//
//    @Override
//    void checkoutMergeFromReleaseBranch() {
//        checkout(workingBranch)
//        merge(getReleaseBranch())
//    }
//
//    @Override
//    void checkoutReleaseBranch() {
//        checkout(getReleaseBranch())
//    }
//
//    String getReleaseBranch() {
//        return pushReleaseVersionBranch ? "release/${getReleaseVersion().replace("-SNAPSHOT", "")}" : workingBranch
//    }
//
//    private checkout(String branch) {
//        exec(['git', 'fetch'], directory: workingDirectory, errorPatterns: ['error: ', 'fatal: '])
//        exec(['git', 'branch', branch], directory: workingDirectory, failOnStderr: false)
//        exec(['git', 'checkout', branch], directory: workingDirectory, errorPatterns: ['error: ', 'fatal: '])
//    }
//
//    private merge(String fromBranch) {
//        exec(['git', 'merge', '--no-ff', '--no-commit', fromBranch], directory: workingDirectory, errorPatterns: ['error: ', 'fatal: ', 'CONFLICT'])
//    }
//
//    private boolean shouldPush() {
//        def shouldPush = false
//        if (extension.git.pushToRemote) {
//            exec(['git', 'remote'], directory: workingDirectory).eachLine { line ->
//                Matcher matcher = line =~ ~/^\s*(.*)\s*$/
//                if (matcher.matches() && matcher.group(1) == extension.git.pushToRemote) {
//                    shouldPush = true
//                }
//            }
//            if (!shouldPush && extension.git.pushToRemote != 'origin') {
//                throw new GradleException("Could not push to remote ${extension.git.pushToRemote} as repository has no such remote")
//            }
//        }
//
//        shouldPush
//    }


    private fun gitStatus(): Map<String, List<String>> {
        return exec(mapOf("directory" to workingDirectory), listOf("git", "status", "--porcelain")).split("\n").groupBy {
            if (it.matches(Regex("""^\s*\?{2}.*"""))) {
                UNVERSIONED
            } else {
                UNCOMMITTED
            }
        }
    }

    private fun gitRemoteStatus() :Map<String, Integer>{
        val branchStatus = exec(mapOf("directory" to workingDirectory), listOf("git", "status", "--porcelain", "-b")).split("\n")[0]
        val aheadRegex = Regex(""".*ahead (\d+).*""")
        val aheadMatcher = aheadRegex.find(branchStatus)

        val behindRegex = Regex(""".*behind (\d+).*""")
        val behindMatcher = behindRegex.matchEntire(branchStatus)

        val remoteStatus = mutableMapOf()

        aheadMatcher?.let {
            remoteStatus[AHEAD] = aheadMatcher[0][1]
        }
        if (behindMatcher.matches()) {
            remoteStatus[BEHIND] = behindMatcher[0][1]
        }
        remoteStatus
    }
}
