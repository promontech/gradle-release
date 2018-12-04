/*
 * This file is part of the gradle-release plugin.
 *
 * (c) Eric Berry
 * (c) ResearchGate GmbH
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package net.researchgate.release

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.api.tasks.Copy

class ReleasePluginKotlin : PluginHelper(), Plugin<Project> {

    companion object {
        val RELEASE_GROUP: String = "Release"
        val PROMOTE_GROUP: String = "Promote"
    }

    private lateinit var scmAdapter: BaseScmAdapter

    override fun apply(project: Project) {
        this.project = project
        extension = project.extensions.create("release", ReleaseExtension::class.java, project, attributes)

        val preCommitText: String? = findProperty("release.preCommitText", null, "preCommitText")
        if (preCommitText != null) {
            extension.preCommitText = preCommitText
        }

        // name tasks with an absolute path so subprojects can be released independently
        var p: String = project.path
        p = if (!p.endsWith(Project.PATH_SEPARATOR)) p + Project.PATH_SEPARATOR else p
//        project.tasks.registering(Jar::class) {
//            dependsOn("classes")
//            classifier = "sources"
//            val sourceSets = project.extensions.getByType<SourceSetContainer>()
//            from(sourceSets.named("main").get().allSource)
//        }
//        with(project) {
//            tasks {
//                register {
//
//                }
//            }
//        }
        project.tasks.register<Copy>("myCopyTask") {
            group = "sample"
            from("build.gradle.kts")
            into("build/copy")
        }
    }
}
//        project.task(mapOf("description" to "Verify project, cut release branch, and update version to next.",
//                "group" to RELEASE_GROUP, "type" to GradleBuild::class.java), "release") {
            //            startParameter = project.getGradle().startParameter.newInstance()

//            tasks = [
//                "${p}createScmAdapter" as String,
//                "${p}initScmAdapter" as String,
//                "${p}checkCommitNeeded" as String,
//                "${p}checkUpdateNeeded" as String,
//                "${p}checkoutMergeToReleaseBranch" as String,
//                "${p}unSnapshotVersion" as String,
//                "${p}confirmReleaseVersion" as String,
//                "${p}checkSnapshotDependencies" as String,
//                "${p}runBuildTasks" as String,
//                    "${p}preTagCommit" as String,
//                    "${p}createReleaseTag" as String,
//                "${p}checkoutMergeFromReleaseBranch" as String,
//                "${p}updateVersion" as String,
//                "${p}commitNewVersion" as String
//            ]
//        }
//        project.task("blah", description: "Verify project, cut release branch, and update version to next.", group: RELEASE_GROUP, type: GradleBuild) doLast {
//            println("TESTING")
//            println("extension ${extension.useAutomaticVersion}")
//        }
//
//        project.task("promote", description: "Tag end of release, promote to release repo", group: RELEASE_GROUP, type: GradleBuild) {
//            startParameter = project.getGradle().startParameter.newInstance()
//
//            tasks = [
//                "${p}createScmAdapter" as String,
//                "${p}initScmAdapter" as String,
//                "${p}checkCommitNeeded" as String,
//                "${p}checkUpdateNeeded" as String,
//                "${p}unSnapshotVersion" as String,
//                "${p}isReleaseOrHotfixBranch" as String,
//                "${p}confirmReleaseVersion" as String,
//                "${p}checkSnapshotDependencies" as String,
//                "${p}runBuildTasks" as String,
//                "${p}preTagCommit" as String,
//                "${p}createReleaseTag" as String,
//                "${p}checkoutMergeFromReleaseBranch" as String,
//                "${p}updateVersion" as String,
//                "${p}commitNewVersion" as String
//            ]
//        }
//
//        CreateScmAdapterTask createScmAdapterTask = project . task ("createScmAdapter", type: CreateScmAdapterTask.class) {
//            group = RELEASE_GROUP
//            description = "Finds the correct SCM plugin"
//        }
//        project.task("initScmAdapter", type: InitScmAdapterTask.class) {
//            group = RELEASE_GROUP
//            description = "Initializes the SCM plugin"
//        }
//        project.task("checkCommitNeeded", type: CheckCommitNeededTask.class, dependsOn: createScmAdapterTask) {
//            group = RELEASE_GROUP
//            description = "Checks to see if there are any added, modified, removed, or un-versioned files."
//        }
//        project.task("checkUpdateNeeded", type: CheckUpdateNeededTask.class, dependsOn: createScmAdapterTask) {
//            group = RELEASE_GROUP
//            description = "Checks to see if there are any incoming or outgoing changes that haven\"t been applied locally."
//        }
//        project.task("checkoutMergeToReleaseBranch", type: CheckoutMergeToReleaseBranchTask.class) {
//            group = RELEASE_GROUP
//            description = "Checkout to the release branch, and merge modifications from the main branch in working tree."
//            onlyIf
//            {
//                extension.pushReleaseVersionBranch
//            }
//        }
//        project.task("unSnapshotVersion", group: RELEASE_GROUP,
//                description: "Removes "-SNAPSHOT" from your project\"s current version.") doLast this.&unSnapshotVersion
//        project.task("isReleaseOrHotfixBranch", group: RELEASE_GROUP,
//                description: "Verify repo is on release or hotfix branch to promote") doLast this.&isReleaseOrHotfixBranch
//        project.task("confirmReleaseVersion", group: RELEASE_GROUP,
//                description: "Prompts user for this release version. Allows for alpha or pre releases.") doLast this.&confirmReleaseVersion
//        project.task("checkSnapshotDependencies", group: RELEASE_GROUP,
//                description: "Checks to see if your project has any SNAPSHOT dependencies.") doLast this.&checkSnapshotDependencies
//
//        project.task("runBuildTasks", group: RELEASE_GROUP,
//                description: "Runs the build process in a separate gradle run.", type: GradleBuild) {
//            startParameter = project.getGradle().startParameter.newInstance()
//
//            project.afterEvaluate {
//                tasks = [
//                    "${p}beforeReleaseBuild" as String,
//                    extension.buildTasks.collect { p + it },
//                    "${p}afterReleaseBuild" as String
//                ].flatten()
//            }
//        }
//        project.task("preTagCommit", group: RELEASE_GROUP,
//                description: "Commits any changes made by the Release plugin - eg. If the unSnapshotVersion task was executed") doLast this.&preTagCommit
//        project.task("createReleaseTag", group: RELEASE_GROUP,
//                description: "Creates a tag in SCM for the current (un-snapshotted) version.") doLast this.&commitTag
//        project.task("checkoutMergeFromReleaseBranch", group: RELEASE_GROUP,
//                description: "Checkout to the main branch, and merge modifications from the release branch in working tree.") {
//            doLast this.& checkoutAndMergeFromReleaseBranch
//                onlyIf {
//                    extension.pushReleaseVersionBranch
//                }
//        }
//        project.task("updateVersion", group: RELEASE_GROUP,
//                description: "Prompts user for the next version. Does it\"s best to supply a smart default.") doLast this.&updateVersion
//        project.task("commitNewVersion", group: RELEASE_GROUP,
//                description: "Commits the version update to your SCM") doLast this.&commitNewVersion
//
//        Boolean supportsMustRunAfter = project . tasks . initScmAdapter . respondsTo ("mustRunAfter")
//
//        if (supportsMustRunAfter) {
//            project.tasks.initScmAdapter.dependsOn(project.tasks.createScmAdapter)
//            project.tasks.checkCommitNeeded.mustRunAfter(project.tasks.initScmAdapter)
//            project.tasks.checkUpdateNeeded.mustRunAfter(project.tasks.checkCommitNeeded)
//            project.tasks.checkoutMergeToReleaseBranch.mustRunAfter(project.tasks.checkUpdateNeeded)
//            project.tasks.unSnapshotVersion.mustRunAfter(project.tasks.checkoutMergeToReleaseBranch)
//            project.tasks.confirmReleaseVersion.mustRunAfter(project.tasks.unSnapshotVersion)
//            project.tasks.checkSnapshotDependencies.mustRunAfter(project.tasks.confirmReleaseVersion)
//            project.tasks.runBuildTasks.mustRunAfter(project.tasks.checkSnapshotDependencies)
//            project.tasks.preTagCommit.mustRunAfter(project.tasks.runBuildTasks)
//            project.tasks.createReleaseTag.mustRunAfter(project.tasks.preTagCommit)
//            project.tasks.checkoutMergeFromReleaseBranch.mustRunAfter(project.tasks.createReleaseTag)
//            project.tasks.updateVersion.mustRunAfter(project.tasks.checkoutMergeFromReleaseBranch)
//            project.tasks.commitNewVersion.mustRunAfter(project.tasks.updateVersion)
//        }
//
//        project.task("beforeReleaseBuild", group: RELEASE_GROUP,
//                description: "Runs immediately before the build when doing a release") {}
//        project.task("afterReleaseBuild", group: RELEASE_GROUP,
//                description: "Runs immediately after the build when doing a release") {}
//
//        if (supportsMustRunAfter) {
//            project.afterEvaluate {
//                def buildTasks = extension . buildTasks
//                        if (!buildTasks.empty) {
//                            project.tasks[buildTasks.first()].mustRunAfter(project.tasks.beforeReleaseBuild)
//                            project.tasks.afterReleaseBuild.mustRunAfter(project.tasks[buildTasks.last()])
//                        }
//            }
//        }
//
//        project.gradle.taskGraph.afterTask {
//            Task task, TaskState state ->
//            if (state.failure && task.name == "release") {
//                try {
//                    createScmAdapter()
//                } catch (Exception ignored) {
//                }
//                if (scmAdapter && extension.revertOnFail && project.file(extension.versionPropertyFile)?.exists()) {
//                    log.error("Release process failed, reverting back any changes made by Release Plugin.")
//                    scmAdapter.revert()
//                } else {
//                    log.error("Release process failed, please remember to revert any uncommitted changes made by the Release Plugin.")
//                }
//            }
//        }
//    }
//
//    void createScmAdapter()
//    {
//        scmAdapter = findScmAdapter()
//    }
//
//    void checkoutAndMergeToReleaseBranch()
//    {
//        if (extension.pushReleaseVersionBranch && !extension.failOnCommitNeeded) {
//            log.warn("/!\\Warning/!\\")
//            log.warn("It is strongly discouraged to set failOnCommitNeeded to false with pushReleaseVersionBranch is enabled.")
//            log.warn("Merging with an uncleaned working directory will lead to unexpected results.")
//        }
//
//        scmAdapter.checkoutMergeToReleaseBranch()
//    }
//
//    void checkoutAndMergeFromReleaseBranch()
//    {
//        if (extension.pushReleaseVersionBranch && !extension.failOnCommitNeeded) {
//            log.warn("/!\\Warning/!\\")
//            log.warn("It is strongly discouraged to set failOnCommitNeeded to false with pushReleaseVersionBranch is enabled.")
//            log.warn("Merging with an uncleaned working directory will lead to unexpected results.")
//        }
//
//        scmAdapter.checkoutMergeFromReleaseBranch()
//    }
//
//    void checkSnapshotDependencies()
//    {
//        def matcher = { Dependency d -> d.version?.contains("SNAPSHOT") && !extension.ignoredSnapshotDependencies.contains("${d.group ?: ""}:${d.name}".toString()) }
//        def collector = { Dependency d -> "${d.group ?: ""}:${d.name}:${d.version ?: ""}" }
//
//        def message = ""
//
//        project.allprojects.each { project ->
//            def snapshotDependencies =[] as Set
//            project.configurations.each { cfg ->
//                snapshotDependencies += cfg.dependencies?.matching(matcher)?.collect(collector)
//            }
//            project.buildscript.configurations.each { cfg ->
//                snapshotDependencies += cfg.dependencies?.matching(matcher)?.collect(collector)
//            }
//            if (snapshotDependencies.size() > 0) {
//                message += "\n\t${project.name}: ${snapshotDependencies}"
//            }
//        }
//
//        if (message) {
//            message = "Snapshot dependencies detected: ${message}"
//            warnOrThrow(extension.failOnSnapshotDependencies, message)
//        }
//    }
//
//    void commitTag()
//    {
//        def message = extension . tagCommitMessage +" "${ tagName() }"."
//        if (extension.preCommitText) {
//            message = "${extension.preCommitText} ${message}"
//        }
//        scmAdapter.createReleaseTag(message)
//    }
//
//    void confirmReleaseVersion()
//    {
//        if (attributes.propertiesFileCreated) {
//            return
//        }
//        updateVersionProperty(getReleaseVersion())
//    }
//
//    void unSnapshotVersion()
//    {
//        checkPropertiesFile()
//        def version = project . version . toString ()
//
//        if (version.contains("-SNAPSHOT")) {
//            attributes.usesSnapshot = true
//            version -= "-SNAPSHOT"
//            updateVersionProperty(version)
//        }
//        scmAdapter.commit(extension.releaseVersionCommitMessage + " "$ { tagName() }".")
//    }
//
//
//    void checkoutReleaseOrHotfixBranch()
//    {
//        def currentBranch = scmAdapter . getBranch ()
//        if (extension.releaseBranchPatterns.any { currentBranch.matches(it) }) {
//            return
//        }
//        return
//    }
//
//    boolean isReleaseOrHotfixBranch()
//    {
//        def currentBranch = scmAdapter . getBranch ()
//        if (extension.releaseBranchPatterns.any { currentBranch.matches(it) }) {
//            return true
//        }
//        return false
//    }
//
//    void preTagCommit()
//    {
//        if (attributes.usesSnapshot || attributes.versionModified || attributes.propertiesFileCreated) {
//            // should only be committed if the project was using a snapshot version.
//            def message = extension . preTagCommitMessage +" "${ tagName() }"."
//
//            if (extension.preCommitText) {
//                message = "${extension.preCommitText} ${message}"
//            }
//
//            if (attributes.propertiesFileCreated) {
//                scmAdapter.add(findPropertiesFile());
//            }
//            scmAdapter.commit(message)
//        }
//    }
//
//    void updateVersion()
//    {
//        def version = project . version . toString ()
//        Map<String, Closure> patterns = extension . versionPatterns
//
//                for (entry in patterns) {
//
//                    String pattern = entry . key
//                            Closure handler = entry . value
//                            Matcher matcher = version =~ pattern
//
//                    if (matcher.find()) {
//                        String nextVersion = handler (matcher, project)
//                        if (attributes.usesSnapshot) {
//                            nextVersion += "-SNAPSHOT"
//                        }
//
//                        nextVersion = getNextVersion(nextVersion)
//                        updateVersionProperty(nextVersion)
//
//                        return
//                    }
//                }
//
//        throw new GradleException ("Failed to increase version [$version] - unknown pattern")
//    }
//
//    String getNextVersion(String candidateVersion)
//    {
//        String nextVersion = findProperty ("release.newVersion", null, "newVersion")
//
//        if (useAutomaticVersion()) {
//            return nextVersion ?: candidateVersion
//        }
//
//        return readLine("Enter the next version (current one released as [${project.version}]):", nextVersion ?: candidateVersion)
//    }
//
//    String getReleaseVersion()
//    {
//        String releaseVersion = findProperty ("release.version")
//
//        if (extension.useAutomaticVersion) {
//            if (isReleaseOrHotfixBranch()) {
//                return scmAdapter.getBranch()
//            } else if (releaseVersion) {
//                return releaseVersion
//            } else {
//                throw new GradleException ("Repository must match a releaseBranchPattern.\nCurrent branch is "$currentBranch"\nCurrent patterns are [${extension.releaseBranchPatterns.join(", ")}]")
//            }
//        }
//
//        return readLine("Enter the release version (current one released as [${project.version}]):", releaseVersion ?: candidateVersion)
//    }
//
//    def commitNewVersion()
//    {
//        def message = extension . newVersionCommitMessage +" "${ tagName() }"."
//        if (extension.preCommitText) {
//            message = "${extension.preCommitText} ${message}"
//        }
//        scmAdapter.commit(message)
//    }
//
//
//    def checkPropertiesFile()
//    {
//        File propertiesFile = findPropertiesFile ()
//
//        if (!propertiesFile.canRead() || !propertiesFile.canWrite()) {
//            throw new GradleException ("Unable to update version property. Please check file permissions.")
//        }
//
//        Properties properties = new Properties()
//        propertiesFile.withReader { properties.load(it) }
//
//        assert properties . version, "[$propertiesFile.canonicalPath] contains no 'version' property"
//        assert extension . versionPatterns . keySet ().any { (properties.version = ~ it).find() },
//        "[$propertiesFile.canonicalPath] version [$properties.version] doesn't match any of known version patterns: " +
//                extension.versionPatterns.keySet()
//
//        // set the project version from the properties file if it was not otherwise specified
//        if (!isVersionDefined()) {
//            project.version = properties.version
//        }
//    }
//
//    /**
//     * Recursively look for the type of the SCM we are dealing with, if no match is found look in parent directory
//     * @param directory the directory to start from
//     */
//    protected BaseScmAdapter findScmAdapter()
//    {
//        BaseScmAdapter adapter
//                File projectPath = project . projectDir . canonicalFile
//
//                extension.scmAdapters.find {
//                    assert BaseScmAdapter . isAssignableFrom (it)
//
//                    BaseScmAdapter instance = it . getConstructor (Project.class, Map .class).newInstance(project, attributes)
//                    if (instance.isSupported(projectPath)) {
//                        adapter = instance
//                        return true
//                    }
//
//                    return false
//                }
//
//        if (adapter == null) {
//            throw new GradleException (
//                    "No supported Adapter could be found. Are [${projectPath}] or its parents are valid scm directories?")
//        }
//
//        adapter
//    }
//    }
