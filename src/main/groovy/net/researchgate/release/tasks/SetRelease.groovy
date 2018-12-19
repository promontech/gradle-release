package net.researchgate.release.tasks

import net.researchgate.release.BranchType
import org.gradle.api.tasks.TaskAction

class SetRelease extends BaseReleaseTask {

    SetRelease() {
        super()
        description = 'Set the BranchType to RELEASE'
    }

    @TaskAction
    def performTask() {
        Map<String, Object> projectAttributes = extension.getOrCreateProjectAttributes(project.name)
        projectAttributes.branchType = BranchType.RELEASE
    }
}
