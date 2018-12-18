package net.researchgate.release.tasks

import net.researchgate.release.BranchType
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

class ConfirmReleaseVersion extends BaseReleaseTask {

    @Inject
    ConfirmReleaseVersion() {
        super()
        description = 'Prompts user for this release version. Allows for alpha or pre releases.'
    }

    @TaskAction
    def confirmReleaseVersion() {
        Map<String, Object> projectAttributes = extension.getOrCreateProjectAttributes(project.name)
        projectAttributes.branchType = BranchType.RELEASE
        if (extension.isUseMultipleVersionFiles()) {
            rootProject.subprojects
                    .findAll { subProject -> !extension.skipRelease(subProject)}
                    .each { subProject ->
                        if (extension.getOrCreateProjectAttributes(subProject.name).propertiesFileCreated) {
                            return
                        }
                        updateVersionProperty(subProject, getReleaseVersion(subProject))
                    }
        } else {
            if (projectAttributes.propertiesFileCreated) {
                return
            }
            updateVersionProperty(rootProject, getReleaseVersion(rootProject))
        }
    }

}
