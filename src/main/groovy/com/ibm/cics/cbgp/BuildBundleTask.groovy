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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.internal.file.copy.DefaultFileCopyDetails
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class BuildBundleTask extends DefaultTask {

    public static final String MISSING_JVMSERVER = 'Specify defaultjvmserver for build'
    public static final String PLEASE_SPECIFY = 'Please specify build configuration'
    public static final String BUILD_CONFIG_EXCEPTION = PLEASE_SPECIFY + """\

Example:
     ${BundlePlugin.BUILD_EXTENSION_NAME} {
       defaultjvmserver = 'EYUCMCIJ'
    } 
"""
    public static final String MISSING_CONFIG = "Define '$BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME' configuration with CICS bundle dependencies"
    public static final String UNSUPPORTED_EXTENSIONS_FOUND = 'Unsupported file extensions for some dependencies, see earlier messages.'
    private static final String EAR = 'ear'
    private static final String JAR = 'jar'
    private static final String WAR = 'war'
    private static final List VALID_DEPENDENCY_FILE_EXTENSIONS = [EAR, JAR, WAR]

    @OutputDirectory
    final DirectoryProperty outputDirectory = project.objects.directoryProperty()

    @TaskAction
    def buildCICSBundle() {
        logger.info "Task ${BundlePlugin.BUILD_TASK_NAME} (Gradle $project.gradle.gradleVersion) "

        def buildExtension = project.extensions.getByName(BundlePlugin.BUILD_EXTENSION_NAME)

        validateBuildExtension(buildExtension)

        // Find & process the configuration
        def foundConfig = project.configurations.find {
            if (it.name == BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME) {
                processCICSBundle(it)
                return true
            }
        }

        if (!foundConfig) {
            println()
            throw new GradleException(MISSING_CONFIG)
        }
    }

    def processCICSBundle(Configuration config) {
        logger.info "processing '$BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME' configuration"
        def filesCopied = []
        project.copy {
            from config
            eachFile {
                logger.lifecycle " Copying $it"
                filesCopied << it
            }
            into outputDirectory
        }
        checkCopiedFileExtensions(filesCopied)
        checkDependenciesCopied(filesCopied, config)
    }

    private void checkDependenciesCopied(List filesCopied, Configuration config) {
        if (config.dependencies.size() == 0) {
            logger.warn "Warning, no external or project dependencies in '$BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME' configuration"
            return
        }

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
                    logger.error " Missing dependency: $dep"
                }
            }
            throw new GradleException("Failed, missing dependencies from '$BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME' configuration")
        }
    }

    private void checkCopiedFileExtensions(List filesCopied) {
        def allExtensionsOk = true
        filesCopied.each() {
            def name = it.name
            def splits = name.split('\\.')
            def extension = splits[splits.length - 1]
            def extensionOK = (splits.size() >= 2 && VALID_DEPENDENCY_FILE_EXTENSIONS.contains(extension))
            if (!extensionOK) {
                logger.error "Unsupported file extension '$extension' for copied dependency '$it.path'"
                allExtensionsOk = false
            }
        }
        if (!allExtensionsOk) {
            throw new GradleException(UNSUPPORTED_EXTENSIONS_FOUND)
        }
    }

    private void validateBuildExtension(buildExtension) {
        def blockValid = true

        // Validate block items exist, no check on content
        if (buildExtension.defaultjvmserver.length() == 0) {
            logger.error MISSING_JVMSERVER
            blockValid = false
        }

        // Throw exception if anything is wrong in the extension block
        if (!blockValid) {
            throw new GradleException(BUILD_CONFIG_EXCEPTION)
        }
    }
}