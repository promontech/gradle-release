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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Ignore

class GitReleasePluginCommitNewVersionTests extends GitSpecification {

    Project project

    FileFilter filter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            String name = pathname.getName().toLowerCase();
            return name.endsWith(".properties") && pathname.isFile();
        }
    }

    def setup() {
        project = ProjectBuilder.builder().withName("GitReleasePluginTest").withProjectDir(localGit.repository.workTree).build()
        project.version = "1.1"
        project.apply plugin: ReleasePlugin
        project.createScmAdapter.execute()
    }

    def cleanup() {
        gitCheckoutBranch(localGit)
        gitCheckoutBranch(remoteGit)
    }

    def 'should push new version to remote tracking branch by default'() {
        given:
        project.file('gradle.properties').withWriter { it << "version=${project.version}" }
        when:
        project.commitNewVersion.execute()
        gitHardReset(localGit)
        then: 'remote repo contains updated properties file'
        localGit.repository.workTree.listFiles(filter).any { it.text.contains("version=$project.version") }
    }

    // TODO fix this test for new workflow
    @Ignore
    def 'should push new version to branch using the branch prefix when it is specified'() {
        given:
        project.file('gradle.properties').withWriter { it << "version=${project.version}" }
        project.release.git.pushToBranchPrefix = 'refs/for/'
        when:
        project.commitNewVersion.execute()
//        gitCheckoutBranch(remoteGit, "refs/for/${project.version}")
        then: 'remote repo contains updated properties file'
        remoteGit.repository.workTree.listFiles(filter).any { it.text.contains("version=$project.version") }
    }

    def 'should only push the version file to branch when pushVersionFileOnly is true'() {
        given:
        project.file('gradle.properties').withWriter { it << "version=${project.version}" }
        gitAdd(localGit, 'test.txt') {
            it << 'testTarget'
        }
        project.file('test.txt').withWriter { it << "testTarget" }
        project.release.git.commitVersionFileOnly = true
        when:
        project.commitNewVersion.execute()
        gitHardReset(remoteGit)
        then: 'remote repo does not get the .gitignore update'
        remoteGit.repository.workTree.listFiles(filter).any { it.text.contains("version=$project.version") }
        ! remoteGit.repository.workTree.listFiles().any { it.name == 'test.txt' && it.text.contains('testTarget') }
    }
}
