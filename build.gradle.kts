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
    id("com.gradle.plugin-publish") version "1.2.0"
    id("signing")
    `kotlin-dsl`
}

group = "com.ibm.cics"
version = "1.0.6-SNAPSHOT"
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
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
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
    implementation("com.ibm.cics:cics-bundle-common:2.0.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.0")
    testImplementation(enforcedPlatform("org.spockframework:spock-bom:2.3-groovy-3.0"))
    testImplementation("org.spockframework:spock-junit4")
    testImplementation("org.spockframework:spock-core")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.register("publishAll") {
    group = "publishing"
    if (isReleaseVersion) {
        dependsOn("publishPlugins") // Publish to Gradle Plugin Portal if a release
    }
    dependsOn("publish") // Publish to Sonatype Snapshots or Central Staging, defined in 'publishing' extension
}

