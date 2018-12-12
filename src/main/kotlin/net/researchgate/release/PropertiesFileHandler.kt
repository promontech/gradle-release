package net.researchgate.release

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.util.*

class PropertiesFileHandler(override var project: Project, override var extension: ReleaseExtension) : PluginHelper() {
    val propertiesFile = project.file(extension.versionPropertyFile)

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

    // TODO maybe move property stuff here???
//    fun iniitialize() {
//        if (!propertiesFile.isFile) {
//            if (!isVersionDefined()) {
//                project.version = getReleaseVersion("1.0.0")
//            }
//
//            if (!useAutomaticVersion() && promptYesOrNo("Do you want to use SNAPSHOT versions in between releases")) {
//                attributes["usesSnapshot"] = true
//            }
//
//            if (useAutomaticVersion() || promptYesOrNo("[$propertiesFile.canonicalPath] not found, create it with version = ${project.version}")) {
//                writeVersion(propertiesFile, "version", project.version)
//                attributes["propertiesFileCreated"] = true
//            } else {
//                log.debug("[$propertiesFile.canonicalPath] was not found, and user opted out of it being created. Throwing exception.")
//                throw GradleException("[$propertiesFile.canonicalPath] not found and you opted out of it being created,\n please create it manually and specify the version property.")
//            }
//        }
//    }
}
