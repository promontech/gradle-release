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

import groovy.text.SimpleTemplateEngine
import net.researchgate.release.cli.Executor
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.slf4j.Logger
import java.io.File

open class PluginHelper {

//    companion object {
        private val LINE_SEP: String = System.getProperty("line.separator")
        private val PROMPT: String = "${LINE_SEP}??>"
//    }

    protected open lateinit var project: Project

    protected lateinit var extension: ReleaseExtension

    protected val executor: Executor by lazy { Executor(project.logger) }

    protected open var attributes: MutableMap<String, Any> = mutableMapOf()

    /**
     * Retrieves SLF4J {@link Logger} instance.
     *
     * The logger is taken from the {@link Project} instance if it's initialized already
     * or from SLF4J {@link LoggerFactory} if it's not.
     *
     * @return SLF4J {@link Logger} instance
     */
    val log: Logger
        get() = project.logger

    fun useAutomaticVersion() =
            findProperty("release.useAutomaticVersion", null, "gradle.release.useAutomaticVersion") == "true"

    /**
     * Executes command specified and retrieves its "stdout" output.
     *
     * @param failOnStderr whether execution should fail if there's any "stderr" output produced, "true" by default.
     * @param commands commands to execute
     * @return command "stdout" output
     */
    fun exec(options: MutableMap<Any, Any> = mutableMapOf(), commands: List<String>): String {
//        initExecutor() // TODO tyler I don't think I need this
        options["directory"] = options["directory"] ?: project.rootDir
        return executor.exec(options, commands)
    }

    //TODO tyler I don't think I need this
//    private fun initExecutor() {
//        if (!::executor.isInitialized) {
//            executor = Executor(log)
//        }
//    }
//
    fun findPropertiesFile(): File {
        val propertiesFile: File = project.file(extension.versionPropertyFile)
        if (!propertiesFile.isFile) {
            if (!isVersionDefined()) {
                project.version = getReleaseVersion("1.0.0")
            }

            if (!useAutomaticVersion() && promptYesOrNo("Do you want to use SNAPSHOT versions inbetween releases")) {
                attributes["usesSnapshot"] = true
            }

            if (useAutomaticVersion() || promptYesOrNo("[$propertiesFile.canonicalPath] not found, create it with version = ${project.version}")) {
                writeVersion(propertiesFile, "version", project.version)
                attributes["propertiesFileCreated"] = true
            } else {
                log.debug("[$propertiesFile.canonicalPath] was not found, and user opted out of it being created. Throwing exception.")
                throw GradleException("[$propertiesFile.canonicalPath] not found and you opted out of it being created,\n please create it manually and specify the version property.")
            }
        }
        return propertiesFile
    }

    protected fun writeVersion(file: File, key: String, version: Any) {
//        try {
        if (!file.isFile) {
            file.parentFile.mkdirs()
            file.writeText("$key=$version")
        } else {
            val newLines = file.readLines().map {
                val pattern = """^(\s*)$key((\s*[=|:]\s*)|(\s+)).+${'$'}"""
                if (it.matches(Regex(pattern))) {
                    it.replace(pattern, """\1$key\2$version""")
                } else {
                    it
                }
            }
            file.writeText(newLines.joinToString("\n")) // TODO tyler test if this works.
            //  also test if we need to catch an exception here
            // we use replace here as other ant tasks escape and modify the whole file
//                project.ant.replaceregexp(file: file, byline: true) {
//                    regexp(pattern: "^(\\s*)$key((\\s*[=|:]\\s*)|(\\s+)).+\$")
//                    substitution(expression: "\\1$key\\2$version")
//                }
        }
//        } catch (be: BuildException) {
//            throw GradleException ("Unable to write version property.", be)
//        }
    }

    fun isVersionDefined() = Project.DEFAULT_VERSION != project.version

    fun warnOrThrow(doThrow: Boolean, message: String) {
        if (doThrow) {
            throw GradleException(message)
        } else {
            log.warn("!!WARNING!! $message")
        }
    }

    fun tagName(): String {
        return if (!extension.tagTemplate.isNullOrBlank()) {
            val engine = SimpleTemplateEngine()
            val binding = mapOf(
                    "version" to project.version,
                    "name" to project.name
            )
            engine.createTemplate(extension.tagTemplate).make(binding).toString()
        } else {
            "${project.version}"
        }

    }

    fun findProperty(key: String, defaultVal: String? = null, deprecatedKey: String? = null): String? {
        var property: String? = System.getProperty(key) ?: project.findProperty(key) as String?

        if (!property.isNullOrBlank() && !deprecatedKey.isNullOrBlank()) {
            property = System.getProperty(deprecatedKey) ?: project.findProperty(deprecatedKey) as String?
            if (!property.isNullOrBlank()) {
                log.warn("You are using the deprecated parameter '${deprecatedKey}'. Please use the new parameter '$key'. The deprecated parameter will be removed in 3.0")
            }
        }

        return property ?: defaultVal
    }

    fun getReleaseVersion(candidateVersion: String = "${project.version}"): String {
        val releaseVersion: String? = findProperty("release.releaseVersion', null, 'releaseVersion")

        if (useAutomaticVersion()) {
            return releaseVersion ?: candidateVersion
        }

        return readLine("This release version:", releaseVersion ?: candidateVersion)
    }

    /**
     * Updates properties file (<code>gradle.properties</code> by default) with new version specified.
     * If configured in plugin convention then updates other properties in file additionally to <code>version</code> property
     *
     * @param newVersion new version to store in the file
     */
    fun updateVersionProperty(newVersion: String) {
        val oldVersion = project.version as String
        if (oldVersion != newVersion) {
            project.version = newVersion
            attributes["versionModified"] = true
            project.subprojects?.forEach { it.version = newVersion } // TODO tyler test that subprojects can't be null and that versions are written correctly
            val versionProperties : List<String> = extension.versionProperties + "version"
            versionProperties.forEach { writeVersion(findPropertiesFile(), it, project.version) }
        }
    }

    /**
     * Reads user input from the console.
     *
     * @param message Message to display
     * @param defaultValue (optional) default value to display
     * @return User input entered or default value if user enters no data
     */
    fun readLine(message: String, defaultValue: String? = null): String {
        val msg = "$PROMPT $message" + (if (defaultValue.isNullOrBlank()) "" else " [$defaultValue] ")
        if (System.console() != null) {
            return System.console().readLine(msg)!! ?: defaultValue!! // todo tyler
        }
        println("$msg (WAITING FOR INPUT BELOW)")

        return System.`in`.reader().readLines()[0] ?: defaultValue!! // todo tyler
    }

    private fun promptYesOrNo(message: String, defaultValue: Boolean = false): Boolean {
        val defaultStr = if (defaultValue) "Y" else "n"
        val consoleVal = readLine("${message} (Y|n)", defaultStr)
        if (consoleVal.isNotBlank()) {
            return consoleVal.toLowerCase().startsWith("y")
        }

        return defaultValue
    }
}
