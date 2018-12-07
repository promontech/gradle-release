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
import org.gradle.kotlin.dsl.get
import java.io.File

abstract class BaseScmAdapter(override var project: Project, override var attributes: MutableMap<String, Any>) : PluginHelper() {

    init {
        extension = project.extensions["release"] as ReleaseExtension
    }

    abstract fun createNewConfig(): Any

    abstract fun isSupported(directory: File): Boolean

    abstract fun init()

    abstract fun checkCommitNeeded()

    abstract fun checkUpdateNeeded()

    abstract fun createReleaseTag(message: String)

    abstract fun add(file: File)

    abstract fun commit(message: String)

    open fun getBranch(): String {
        throw GradleException("Getting current branch is currently only supported for GIT projects")
    }

    abstract fun revert()

    open fun checkoutMergeToReleaseBranch() {
        throw GradleException("Checkout and merge is supported only for GIT projects")
    }

    open fun checkoutMergeFromReleaseBranch() {
        throw GradleException("Checkout and merge is supported only for GIT projects")
    }

    open fun checkoutReleaseBranch() {
        throw GradleException("Checkout and merge is supported only for GIT projects")
    }
}
