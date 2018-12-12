package net.researchgate.release.tasks

import groovy.text.SimpleTemplateEngine
import net.researchgate.release.PropertiesFileHandler
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.util.*
import org.gradle.kotlin.dsl.*

/**
 * Check that updates to the project are not needed
 */
open class UnsnapshotVersionTask : BaseReleaseTask() {
//    private var scmAdapter: BaseScmAdapter? = null
//    var extension: ReleaseExtension = project.extensions.getByType(ReleaseExtension::class.java)
//    private val log: Logger = project.logger
    private val propertiesFileHandler = PropertiesFileHandler(project, extension)

    init {
        description = "testing 123"
    }

    @TaskAction
    fun task() {
        if (extension.useMultipleVersionFiles) {
            project.rootProject.subprojects { project: Project ->
                unSnapshotVersion(project)
            }
        } else {
            unSnapshotVersion(getProject())
        }
    }

    fun unSnapshotVersion(projectToUnSnapshot: Project) {
        if (extension.skipRelease(projectToUnSnapshot)) {
            // Replace the version with the latest released version
            val latestTag = scmAdapter.getLatestTag(projectToUnSnapshot.name) ?: throw GradleException("The latest tag of '" + projectToUnSnapshot.name + "' is not available")

            if (!extension.tagTemplate.isNullOrBlank()) {
                throw GradleException("Skipping a release requires the 'tagTemplate' property to be set")
            }

            // Determine the version number part of the tag using the tag template
            val engine = SimpleTemplateEngine()
            val binding = mapOf(
                    "version" to "",
                    "name" to projectToUnSnapshot.name
            )
            val tagNamePart: String = engine.createTemplate(extension.tagTemplate).make(binding).toString()
            val version: String = latestTag.replaceAll(tagNamePart, "")

            log.debug("Using version " + version + " for " + projectToUnSnapshot.name + " dependencies")
            projectToUnSnapshot.version = version
            return
        }
        checkPropertiesFile(projectToUnSnapshot)
        val version = projectToUnSnapshot.version.toString()

        if (version.contains("-SNAPSHOT")) {
            val projectAttributes: Map<String, Any> = extension.getOrCreateProjectAttributes(projectToUnSnapshot.name)
            projectAttributes["usesSnapshot"] = true
            version.replace("-SNAPSHOT","")
            updateVersionProperty(projectToUnSnapshot, version)
        }
    }


    fun checkPropertiesFile() {
        val propertiesFile = findPropertiesFile()
        if (!propertiesFile.canRead() || !propertiesFile.canWrite()) {
            throw GradleException("Unable to update version property. Please check file permissions.")
        }

        val properties = Properties()
        properties.load(propertiesFile.reader())

        val version = properties.getProperty("version") ?: throw GradleException("[$propertiesFile.canonicalPath] contains no 'version' property")
        assert(extension.versionPatterns.keys.any { version.matches(it.toRegex()) }) {
            "[$propertiesFile.canonicalPath] version [$properties.version] doesn't match any of known version patterns: ${extension.versionPatterns.keys}"
        }

        // set the project version from the properties file if it was not otherwise specified
        if (!isVersionDefined()) {
            project.version = version
        }
    }
}
