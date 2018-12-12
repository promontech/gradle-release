package net.researchgate.release.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Set the versions in the gradle.properties file
 */
open class ConsumerTask @Inject constructor(objects: ObjectFactory) : DefaultTask() {
    @Internal
    var updatedPropertiesFile: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun createScmAdapter() {
        println("HI")
        val output = updatedPropertiesFile.get().asFile
        println(output.readLines())
    }
}
