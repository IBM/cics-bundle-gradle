package com.ibm.cics.cbgp

/*-
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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.internal.file.copy.DefaultFileCopyDetails
import org.gradle.api.tasks.TaskAction

class CICSBundleBuilderTask extends DefaultTask {

    public static final String CICS_BUNDLE_CONFIG_NAME = "cicsBundle"

    @TaskAction
    def buildCICSBundle() {
        print "Task buildCICSBundle (Gradle $project.gradle.gradleVersion) "

        // Find & process the configuration
        def foundConfig = false
        project.configurations.each {
            if (it.name == CICS_BUNDLE_CONFIG_NAME) {
                processCICSBundle(it)
                foundConfig = true
            }
        }

        if (!foundConfig) {
            println()
            throw new GradleException("Define \'$CICS_BUNDLE_CONFIG_NAME\' configuration with CICS bundle dependencies")
        }
    }

    def processCICSBundle(Configuration config) {
        println("processing \'$CICS_BUNDLE_CONFIG_NAME\' configuration")
        def filesCopied = []
        project.copy {
            from config
            eachFile {
                println(" Copying $it")
                filesCopied << it
            }
            into "$project.buildDir/$project.name-$project.version"
        }
        checkDependenciesCopied(filesCopied, config)
    }

    private void checkDependenciesCopied(List filesCopied, Configuration config) {
        if (filesCopied.size() < config.dependencies.size()) {
            config.dependencies.each { dep ->
                def foundDependency = false
                for (def copied : filesCopied) {
                    if (copied instanceof DefaultFileCopyDetails) {
                        def copiedFullPath = copied.file.toString()
                        // Check here by dependency type
                        if (dep instanceof DefaultProjectDependency) {
                            if (copiedFullPath.contains(dep.dependencyProject.name)
                                    && copiedFullPath.contains(dep.dependencyProject.version)
                                    && copiedFullPath.contains(dep.dependencyProject.group)) {
                                foundDependency = true;
                                break;
                            }
                        } else if (dep instanceof DefaultExternalModuleDependency) {
                            if (copiedFullPath.contains(dep.name)
                                    && copiedFullPath.contains(dep.version)
                                    && copiedFullPath.contains(dep.group)) {
                                foundDependency = true;
                                break;
                            }
                        } else throw new GradleException("Unexpected dependency type" + dep.class.toString() + "for dependency $dep")
                    }
                }
                if (!foundDependency) {
                    println(" Missing dependency: $dep")
                }
            }
            throw new GradleException("Failed, missing dependencies from '$CICS_BUNDLE_CONFIG_NAME' configuration")
        }
    }
}