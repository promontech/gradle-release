package net.researchgate.release.tasks

import net.researchgate.release.BranchType
import org.gradle.api.tasks.TaskAction

class SetHotfix extends BaseReleaseTask {

    SetHotfix() {
        super()
        description = 'Set the BranchType to HOTFIX'
    }

    @TaskAction
    def performTask() {
        Map<String, Object> projectAttributes = extension.getOrCreateProjectAttributes(project.name)
        projectAttributes.branchType = BranchType.HOTFIX
    }
}
