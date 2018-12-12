package net.researchgate.release.tasks

import groovy.text.SimpleTemplateEngine
import net.researchgate.release.BaseScmAdapter
import net.researchgate.release.ReleaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.slf4j.Logger
import java.io.File

open class BaseReleaseTask : DefaultTask() {

    companion object {
        const val RELEASE_GROUP = "Release"
    }

    private val LINE_SEP = System.getProperty("line.separator")
    val PROMPT = "$LINE_SEP??>"

    var extension: ReleaseExtension
    var pluginAttributes: Map<String, Any>

    init {
        group = RELEASE_GROUP
        extension = project.rootProject.extensions.getByName("release") as ReleaseExtension
        pluginAttributes = extension.attributes
    }

    fun getScmAdapter(): BaseScmAdapter {
        return extension.scmAdapter
    }

    /**
     * Retrieves SLF4J {@link org.slf4j.Logger} instance.
     *
     * The logger is taken from the {@link Project} instance if it's initialized already
     * or from SLF4J {@link org.slf4j.LoggerFactory} if it's not.
     *
     * @return SLF4J {@link org.slf4j.Logger} instance
     */
    val log: Logger = project.logger

    fun useAutomaticVersion() = findProperty("release.useAutomaticVersion") == "true"

    fun findPropertiesFile(project: Project): File {
        val propertiesFile = project.file(extension.versionPropertyFile)
        val projectAttributes: MutableMap<String, Any> = extension.getOrCreateProjectAttributes(project.name)
        if (!propertiesFile.isFile) {
            if (!isVersionDefined()) {
                project.version = getReleaseVersion(project, "1.0.0")
            }

            if (!useAutomaticVersion() && promptYesOrNo("Do you want to use SNAPSHOT versions in between releases")) {
                projectAttributes["usesSnapshot"] = true
            }

            if (useAutomaticVersion() || promptYesOrNo("[$propertiesFile.canonicalPath] not found, create it with version = ${project.version}")) {
                writeVersion(propertiesFile, "version", project.version)
                projectAttributes["propertiesFileCreated"] = true
            } else {
                log.debug("[$propertiesFile.canonicalPath] was not found, and user opted out of it being created. Throwing exception.")
                throw GradleException("[$propertiesFile.canonicalPath] not found and you opted out of it being created,\n please create it manually and specify the version property.")
            }
        }
        return propertiesFile
    }


    protected fun writeVersion(file: File, key: String, version: Any) {
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
        }
    }
    //Old version
//    protected fun writeVersion(file: File, key: String, version: String) {
//        try {
//            if (!file.isFile) {
//                project.ant.echo(file: file, message: "$key=$version")
//            } else {
//                 we use replace here as other ant tasks escape and modify the whole file
//                getProject().ant.replaceregexp(file: file, byline: true) {
//                    regexp(pattern: "^(\\s*)$key((\\s*[=|:]\\s*)|(\\s+)).+\$")
//                    substitution(expression: "\\1$key\\2$version")
//                }
//            }
//        } catch (BuildException be) {
//            throw new GradleException("Unable to write version property.", be)
//        }
//    }

    fun isVersionDefined() = Project.DEFAULT_VERSION != project.version


    fun warnOrThrow(doThrow: Boolean, message: String) {
        if (doThrow) {
            throw GradleException(message)
        } else {
            log.warn("!!WARNING!! $message")
        }
    }

    fun tagName(): String {
        return if (!extension.tagTemplate.isBlank()) {
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

    fun isMultiVersionProject() = extension.useMultipleVersionFiles

    fun getReleaseVersion(project: Project, candidateVersion: String = "${project.version}"): String {
        val key = if (isMultiVersionProject()) "release." + project.name + ".releaseVersion" else "release.releaseVersion"
        val releaseVersion: String? = findProperty(key, null, "releaseVersion")

        if (useAutomaticVersion()) {
            return releaseVersion ?: candidateVersion
        }

        return readLine("This release version for " + project.name + ":", releaseVersion ?: candidateVersion)
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
            val projectAttributes: MutableMap<String, Any> = extension.getOrCreateProjectAttributes(project.name)
            projectAttributes["versionModified"] = true
            if (!isMultiVersionProject()) {
                project.subprojects?.forEach { it.version = newVersion } // TODO tyler test that subprojects can't be null and that versions are written correctly
            }
            val versionProperties: List<String> = extension.versionProperties + "version"
            versionProperties.forEach { writeVersion(findPropertiesFile(project), it, project.version) }
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

        val value = readLine()
        return if (value.isNullOrBlank()) defaultValue!! else value
    }

    fun promptYesOrNo(message: String, defaultValue: Boolean = false): Boolean {
        val defaultStr = if (defaultValue) "Y" else "n"
        val consoleVal = readLine("${message} (Y|n)", defaultStr)
        if (consoleVal.isNotBlank()) {
            return consoleVal.toLowerCase().startsWith("y")
        }

        return defaultValue
    }
}
