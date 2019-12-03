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
    id("org.jetbrains.kotlin.jvm") version "1.3.50"
}

group = "com.ibm.cics"
version = "0.0.1-SNAPSHOT"

gradlePlugin {
    plugins {
        register("cics-bundle-gradle-plugin") {
            id = "cics-bundle-gradle-plugin"
            implementationClass = "com.ibm.cics.cbgp.BundlePlugin"
        }
    }
}

repositories {
    mavenCentral()
    maven {
        // For cics-bundle-common // TODO update when published formally
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

defaultTasks("build")

dependencies {
    implementation("com.ibm.cics:cics-bundle-common:0.0.2")
    testCompile("junit:junit:4.12")
    testImplementation("org.spockframework:spock-core:1.1-groovy-2.4") {
        exclude(module =  "groovy-all")
    }
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
