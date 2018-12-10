package net.researchgate.release/*
 * This file is part of the gradle-release plugin.
 *
 * (c) Eric Berry
 * (c) ResearchGate GmbH
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import org.gradle.api.GradleException
import org.gradle.api.Project

import java.util.regex.Pattern
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

open class ReleaseExtension(val project: Project, val attributes: MutableMap<String, Any>) {

    init {
        println("Attribute $attributes")
    }
    var failOnCommitNeeded: Boolean = true
    var failOnPublishNeeded: Boolean = true
    var failOnSnapshotDependencies: Boolean = true
    var failOnUnversionedFiles: Boolean = true
    var failOnUpdateNeeded: Boolean = true
    var revertOnFail: Boolean = true
    //    @Option(option = 'useAutomaticVersion', description = 'Set the filename of the file to be opened.')
    var useAutomaticVersion: Boolean? = null
    var preCommitText: String = ""
    var preTagCommitMessage: String = "[Gradle Release Plugin] - pre tag commit: "
    var tagCommitMessage: String = "[Gradle Release Plugin] - creating tag: "
    var newVersionCommitMessage: String = "[Gradle Release Plugin] - new version commit: "
    var releaseVersionCommitMessage: String = "[Gradle Release Plugin] - release version commit: "
    var pushReleaseVersionBranch: Boolean = false

    /**
     * as of 3.0 set this to "$version" by default
     */
    val tagTemplate: String? = null
    val versionPropertyFile: String = "gradle.properties"
    val versionProperties = emptyList()
    val buildTasks: List<String> = listOf("build")
    val ignoredSnapshotDependencies = emptyList()
    var versionPatterns: Map<String, (matcher: MatchGroup, p: Project) -> String> = mapOf(
            // Increments last number: "2.5-SNAPSHOT" => "2.6-SNAPSHOT"
//            """(\d+)([^\d]*$)""" to { m, p -> m.replaceAll("${(m[0][1] as int) + 1}${m[0][2]}") } // TODO tyler
    )
    var releaseBranchPatterns: List<String> = listOf(
            """^(release|hotfix).*"""
    )
    var scmAdapters: List<KClass<out BaseScmAdapter>> = listOf(
            GitAdapter::class
//            SvnAdapter::class,
//            HgAdapter::class,
//            BzrAdapter::class
    )

    var git: GitAdapter.GitConfig? = null

//    init {
//        ExpandoMetaClass mc = new ExpandoMetaClass(net.researchgate.release.ReleaseExtension, false, true)
//        mc.initialize()
//        metaClass = mc
//    }

    //
//    def propertyMissing(String name) {
//        if (isDeprecatedOption(name)) {
//            def value = null
//            if (name == "includeProjectNameInTag") {
//                value = false
//            }
//
//            return metaClass."$name" = value
//        }
//        net.researchgate.release.BaseScmAdapter adapter = getAdapterForName(name)
//        Object result = adapter?.createNewConfig()
//
//        if (!adapter || !result) {
//            throw new MissingPropertyException(name, this.class)
//        }
//
//        metaClass."$name" = result
//    }
//
//    def propertyMissing(String name, value) {
//        if (isDeprecatedOption(name)) {
//            project.logger?.warn("You are setting the deprecated option '${name}'. The deprecated option will be removed in 3.0")
//            project.logger?.warn("Please upgrade your configuration to use 'tagTemplate'. See https://github.com/researchgate/gradle-release/blob/master/UPGRADE.md#migrate-to-new-tagtemplate-configuration")
//
//            return metaClass."$name" = value
//        }
//        net.researchgate.release.BaseScmAdapter adapter = getAdapterForName(name)
//
//        if (!adapter) {
//            throw new MissingPropertyException(name, this.class)
//        }
//        metaClass."$name" = value
//    }
//
//    def methodMissing(String name, args) {
//        metaClass."$name" = { Closure varClosure ->
//            return ConfigureUtil.configure(varClosure, this."$name")
//        }
//
//        try {
//            return ConfigureUtil.configure(args[0] as Closure, this."$name")
//        } catch (MissingPropertyException ignored) {
//            throw new MissingMethodException(name, this.class, args)
//        }
//    }
//
    private fun isDeprecatedOption(name: String) = name == "includeProjectNameInTag" || name == "tagPrefix"

    private fun getAdapterForName(name: String): BaseScmAdapter {
        val adapter = scmAdapters.filter { it.isSubclassOf(BaseScmAdapter::class) }.first {
            val pattern: Pattern = Pattern.compile("^$name", Pattern.CASE_INSENSITIVE)
            pattern.matcher(it.simpleName).find()
        }
        return adapter.primaryConstructor?.call(project, attributes)
                ?: throw GradleException("No supported Adapter could be found for $name. Are any of the parents valid scm directories?")
    }
}
