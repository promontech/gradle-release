package net.researchgate.release

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class InitScmAdapter : DefaultTask() {
    private val scmAdapter: BaseScmAdapter = project.tasks.getByPath("createScmAdapter").property("scmAdapter") as BaseScmAdapter
    private val extension: ReleaseExtension = project.extensions.getByType(ReleaseExtension::class.java)

    @TaskAction
    fun createScmAdapter() {
        description = "Finds the correct SCM plugin"
        val projectPath: File = project.projectDir.canonicalFile

        println("initing scm adaptor")
        println(scmAdapter)

//        println(project.objects::class.members)
//        println(project::class.members)
//        println(project.extensions.getByName("test"))
        println(project.tasks.getByPath("createScmAdapter").property("scmAdapter"))
//        println(extension.git.requireBranch)
    }


}
