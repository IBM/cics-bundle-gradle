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
    id("com.gradle.plugin-publish") version "0.12.0"
    id("signing")
    `kotlin-dsl`
}

group = "com.ibm.cics"
version = "1.0.1"
val isReleaseVersion by extra(!version.toString().endsWith("SNAPSHOT"))

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

signing {
    setRequired { !gradle.taskGraph.hasTask(":publishToMavenLocal") }
    sign(publishing.publications)
}

publishing {
    repositories {
        if (isReleaseVersion) {
            maven {
                name = "OSSRH"
                url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = ossrhUser
                    password = ossrhPassword
                }
            }
        } else {
            maven {
                name = "SonatypeSnapshots"
                url = uri("https://oss.sonatype.org/content/repositories/snapshots")
                credentials {
                    username = ossrhUser
                    password = ossrhPassword
                }
            }
        }
    }
}

repositories {
    maven {
        name = "SonatypeSnapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
    mavenCentral()
}

defaultTasks("build")

dependencies {
    implementation("com.ibm.cics:cics-bundle-common:1.0.2")
    testImplementation("junit:junit:4.12")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.25.1")
    testImplementation("org.spockframework:spock-core:1.1-groovy-2.4") {
        exclude(module =  "groovy-all")
    }
}

tasks.register("publishAll") {
    if (isReleaseVersion) {
        dependsOn("publishPlugins") // Publish to Gradle Plugin Portal if a release
    }
    dependsOn("publish") // Publish to Sonatype Snapshots or Central Staging, defined in 'publishing' extension
}
