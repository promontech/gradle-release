package net.researchgate.release

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CreateScmAdapter : DefaultTask() {
    var scmAdapter: BaseScmAdapter? = null
    var extension: ReleaseExtension = project.extensions.getByType(ReleaseExtension::class.java)

    @TaskAction
    fun createScmAdapter() {
        description = "Finds the correct SCM plugin"
        val projectPath: File = project.projectDir.canonicalFile

        println("print message here")
        val instance: Class<out BaseScmAdapter>? = extension.scmAdapters.find {
            assert(BaseScmAdapter::class.java.isAssignableFrom(it))

            val instance: BaseScmAdapter = it.getConstructor(Project::class.java, Map::class.java).newInstance(project,null) //, this.attributes)
            instance.isSupported(projectPath)
        }

        scmAdapter = instance?.getConstructor(Project::class.java, Map::class.java)?.newInstance(project, null)/*extension2.attributes)*/
                ?: throw GradleException("No supported Adapter could be found. Are [${projectPath}] or its parents are valid scm directories?")
        scmAdapter!!.init()
    }


}
