package net.researchgate.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

/**
 * Create the SCM adapter based on the type of SCM in use in the project
 */
open class CreateScmAdapterTask : DefaultTask() {
    var scmAdapter: BaseScmAdapter? = null
    var extension: ReleaseExtension = project.extensions.getByType(ReleaseExtension::class.java)

    @TaskAction
    fun createScmAdapter() {
        val projectPath: File = project.projectDir.canonicalFile

        val instance: KClass<out BaseScmAdapter>? = extension.scmAdapters.find {
            assert(it.isSubclassOf(BaseScmAdapter::class))

            val instance: BaseScmAdapter = it.primaryConstructor?.call(project, extension.attributes)
                    ?: throw GradleException("Failed to call primary constructor with $project and ${extension.attributes}")
            instance.isSupported(projectPath)
        }

        scmAdapter = instance?.primaryConstructor?.call(project, extension.attributes)
                ?: throw GradleException("No supported Adapter could be found. Are [${projectPath}] or its parents are valid scm directories?")
        scmAdapter!!.init()
    }


}
