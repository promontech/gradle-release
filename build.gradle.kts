import nu.studer.gradle.credentials.domain.CredentialsContainer

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath("org.jfrog.buildinfo:build-info-extractor-gradle:4.3.0")
        classpath("net.researchgate:gradle-release:2.7.0")
//        classpath "com.bmuschko:gradle-nexus-plugin:2.3.1"
    }
}

plugins {
    id("org.gradle.java-gradle-plugin")
    id("org.gradle.groovy")
    id("maven-publish")
    id("net.researchgate.release") version "2.7.0"
    id("com.jfrog.bintray") version "1.8.4"
    id("nu.studer.credentials") version "1.0.7"
}

apply(plugin = "idea")
apply(plugin = "maven")
//apply plugin: "com.bmuschko.nexus"

val spockVersion: String by project
val junitVersion: String by project
val jgitVersion: String by project
val cglibVersion: String by project

group = "net.researchgate"

repositories {
    mavenCentral()
}

dependencies {
    compile(gradleApi())
    testCompile("org.spockframework:spock-core:$spockVersion") {
        exclude(module = "groovy-all")
    }
    testCompile("junit:junit:$junitVersion")
    testCompile("org.eclipse.jgit:org.eclipse.jgit:$jgitVersion")
    testCompile("cglib:cglib-nodep:$cglibVersion")
    testCompile(gradleTestKit())
}

gradlePlugin {
    plugins {
        register("releasePlugin") {
            id = "net.researchgate.release"
            implementationClass = "net.researchgate.release.ReleasePlugin"
        }
    }
}

val credentials: CredentialsContainer by project.extra
fun getCredentials(prop: String) = credentials.getProperty(prop) as String?

publishing {
    val version = project.version.toString()
    val (_username, _password, _url) = when {
        version.endsWith("-SNAPSHOT") -> Triple(
            getCredentials("nexusSnapshotUsername"),
            getCredentials("nexusSnapshotPassword"),
            uri(getCredentials("nexusSnapshotDeployUrl")!!)
        )
        else -> Triple(getCredentials("nexusStageUsername"), getCredentials("nexusStagePassword"), uri(getCredentials("nexusStageDeployUrl")!!))
    }

    repositories {
        maven {
            credentials {
                println("version $version ${version.contains("rc")}")
                username = _username
                password = _password
            }
            name = "nexus"
            url = _url
        }
    }
}

tasks["publish"].dependsOn("assemble")
tasks["publishToMavenLocal"].dependsOn("assemble")
