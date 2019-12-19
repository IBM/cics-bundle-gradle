import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * #%L
 * CICS Bundle Gradle Plugin
 * %%
 * Copyright (C) 2019 IBM Corp.
 * %%
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *  
 * SPDX-License-Identifier: EPL-2.0
 * #L%
 */
plugins {
    id("groovy")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "0.10.1"
    `kotlin-dsl`
}

group = "com.ibm.cics"
version = "0.0.1-SNAPSHOT"
val isReleaseVersion by extra(!version.toString().endsWith("SNAPSHOT"))
val onlyIfSnapshot: (PublishToMavenRepository).() -> Unit = {
    this.onlyIf { !isReleaseVersion }
}

// Only publish to Gradle plugin portal if a release
tasks.publishPlugins{ enabled = isReleaseVersion }

// Only publish to Sonatype Snapshots if a snapshot
tasks.withType<org.gradle.api.publish.maven.tasks.PublishToMavenRepository>().configureEach { onlyIfSnapshot }
tasks.withType<org.gradle.api.publish.maven.tasks.GenerateMavenPom>().configureEach { onlyIfSnapshot }

gradlePlugin {
    plugins {
        register("com.ibm.cics.bundle") {
            id = "com.ibm.cics.bundle"
            displayName = "CICS Bundle Gradle Plugin"
            description = "A Gradle plugin to build CICS bundles, including external dependencies."
            implementationClass = "com.ibm.cics.cbgp.BundlePlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/IBM/cics-bundle-gradle"
    vcsUrl = "https://github.com/IBM/cics-bundle-gradle"
    tags = listOf("cics", "cicsts", "cicsbundle", "cics-bundle")
}

val ossrhUser: String? by project
val ossrhPassword: String? by project

publishing {
    repositories {
        maven {
            name = "Sonatype Snapshots"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
            credentials {
                username = ossrhUser
                password = ossrhPassword
            }
        }
    }
}

repositories {
    mavenCentral()
}

defaultTasks("build")

dependencies {
    implementation("com.ibm.cics:cics-bundle-common:0.0.2")
    testCompile("junit:junit:4.12")
    testImplementation("org.spockframework:spock-core:1.1-groovy-2.4") {
        exclude(module =  "groovy-all")
    }
}

tasks.register("publishAll") {
    dependsOn("publishPlugins")
    dependsOn("publish")
}
