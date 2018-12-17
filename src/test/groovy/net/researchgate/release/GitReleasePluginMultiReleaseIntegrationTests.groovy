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

import org.eclipse.jgit.api.Status
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

class GitReleasePluginMultiReleaseIntegrationTests extends GitSpecification {

    File projectDir
    private File subProject2Dir
    private File subProject1Dir

    def setup() {
        projectDir = localGit.repository.getWorkTree()

        gitAddAndCommit(localGit, "settings.gradle") { it << """
            rootProject.name = 'GitReleasePluginTest'
            include 'subproject1', 'subproject2'
        """
        }
        gitAddAndCommit(localGit, "build.gradle") { it << """
             buildscript {
                repositories {
                    flatDir {
                        dirs '${new File('./build/tmp/testJars/').absolutePath}'
                    }
                }
                dependencies {
                    classpath 'net.researchgate:gradle-release:0.0.0'
                }
            }
            apply plugin: 'net.researchgate.release'
            
            project.release {
                useMultipleVersionFiles = true
                buildTasks = [':subproject1:build',':subproject2:build']
            }
            
            project('subproject1') {
                apply plugin: 'java'
            }
            
            project('subproject2') {
                apply plugin: 'java'
            }
        """
        }
        gitAddAndCommit(localGit, '.gitignore') { it << ".gradle/" }

        subProject1Dir = new File(projectDir, "subproject1")
        this.subProject1Dir.mkdirs()
        gitAddAndCommit(localGit, "subproject1/gradle.properties") { it << "version=1.1" }

        subProject2Dir = new File(projectDir, "subproject2")
        this.subProject2Dir.mkdirs()
        gitAddAndCommit(localGit, "subproject2/gradle.properties") { it << "version=2.1" }
    }

    def cleanup() {
        gitCheckoutBranch(localGit)
        gitCheckoutBranch(remoteGit)
    }

    @Override
    def createDefaultVersionFile() {
        return false
    }

    def 'Separate submodules version files should be updated'() {
        given: 'multimodule project'
        localGit.push().setForce(true).call()
        when: 'calling release task indirectly'
        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments('release', '-Prelease.useAutomaticVersion = true')
                .withPluginClasspath()
                .withGradleVersion('4.10.2')
                .build()
        println result.output
        Status st = localGit.status().call()
        gitHardReset(remoteGit)
        then: 'modified files in local repo'
        st.modified.size() == 0 && st.added.size() == 0 && st.changed.size() == 0
        // TODO test tag list in a different integration test
//        and: 'tag with old version 1.1 created in local repo'
//        localGit.tagList().call().any { shortenRefName(it.name) == '1.1' }
        and: 'property file updated to new version in local repo'
        new File(subProject1Dir, 'gradle.properties').text == 'version=1.2'
        and: 'property file with new version pushed to remote repo'
        new File(subProject2Dir, 'gradle.properties').text == 'version=2.2'
        // TODO test tag list in a different integration test
//        and: 'tag with old version 1.1 pushed to remote repo'
//        remoteGit.tagList().call().any { shortenRefName(it.name) == '1.1' }
    }
}
