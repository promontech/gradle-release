package net.researchgate.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File
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

        extension.scmAdapters.find {
            assert(it.isSubclassOf(BaseScmAdapter::class))

            scmAdapter = it.primaryConstructor?.call(project, extension.attributes)
                    ?: throw GradleException("Failed to call primary constructor with $project and ${extension.attributes}")
            scmAdapter?.isSupported(projectPath) ?: false
        }

        scmAdapter ?: throw GradleException("No supported Adapter could be found. Are [${projectPath}] or its parents are valid scm directories?")
        scmAdapter!!.init()
    }


}
