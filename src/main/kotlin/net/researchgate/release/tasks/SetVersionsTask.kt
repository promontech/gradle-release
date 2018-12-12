package net.researchgate.release.tasks

import net.researchgate.release.BaseScmAdapter
import net.researchgate.release.ReleaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import java.io.File
import javax.inject.Inject

/**
 * Set the versions in the gradle.properties file
 */
open class SetVersionsTask @Inject constructor(objects: ObjectFactory) : DefaultTask() {
    private var scmAdapter: BaseScmAdapter? = null
    @Internal
    private var propertiesFile: File = project.file((project.extensions["release"] as ReleaseExtension).versionPropertyFile)

    @OutputFile
    var updatedPropertiesFile: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun createScmAdapter() {
        val message = " HELLOIJDF"
        val output = updatedPropertiesFile.get().asFile
        output.writeText(message)
    }
}
