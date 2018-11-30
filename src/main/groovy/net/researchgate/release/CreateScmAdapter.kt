package net.researchgate.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CreateScmAdapter : DefaultTask() {
//    private val configurableOutputFiles: ConfigurableFileCollection = project.layout.configurableFiles()

    var scmAdapter: BaseScmAdapter? = null
//    var message by project.objects.property<String>()
//    var outputFiles: FileCollection by configurableOutputFiles

    var extension: ReleaseExtension = project.extensions.getByType(ReleaseExtension::class.java)
//    var extension2: PluginHelper = project.parent as PluginHelper

    @TaskAction
    fun createScmAdapter() {
        description = "Finds the correct SCM plugin"

//        println("extension 2 $extension2")
        val projectPath: File = project.projectDir.canonicalFile

        println("print message here")
        val instance: Class<out BaseScmAdapter>? = extension.scmAdapters.find {
            assert(BaseScmAdapter::class.java.isAssignableFrom(it))

            val instance: BaseScmAdapter = it.getConstructor(Project::class.java, Map::class.java).newInstance(project,null) /*extension2.attributes)*/
            instance.isSupported(projectPath)
        }

        scmAdapter = instance?.getConstructor(Project::class.java, Map::class.java)?.newInstance(project, null)/*extension2.attributes)*/
                ?: throw GradleException("No supported Adapter could be found. Are [${projectPath}] or its parents are valid scm directories?")
    }


}
