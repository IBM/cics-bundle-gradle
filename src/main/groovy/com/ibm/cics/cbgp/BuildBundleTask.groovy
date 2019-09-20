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

package com.ibm.cics.cbgp

import com.ibm.cics.bundle.parts.BundlePublisher
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class BuildBundleTask extends AbstractBundleTask {

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

	List resolvedDependencies = []

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

//	@SuppressWarnings("GrUnresolvedAccess")
	def processCICSBundle(Configuration config) {

		logger.info "processing '$BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME' configuration"
		def resolved = config.resolvedConfiguration
		if (resolved.hasError()) {
			throw new GradleException('Failed to resolve cicsBundle dependencies')
		}

		if (resolved.files.size() == 0) {
			logger.warn "Warning, no external or project dependencies in '$BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME' configuration"
			return
		}

		def files = resolved.files

		files.each {
			logger.lifecycle("Resolved file:  $it")
			resolvedDependencies << it
		}

		checkResolvedFileExtensions(resolvedDependencies)
		BundlePublisher bundlePublisher = initBundlePublisher()

//		try {
//			bundlePublisher.publishResources()
//		} catch (BundlePublisher.PublishException e) {
//			throw new GradleException(e.getMessage(), e)
//		}
	}

	private void checkResolvedFileExtensions(List resolvedDependencies) {
		def allExtensionsOk = true
		resolvedDependencies.each() {
			String name = it.name
			int dotpos = name.lastIndexOf('.')
			if (dotpos > -1) {
				String extension = name.substring(dotpos + 1)
				if (VALID_DEPENDENCY_FILE_EXTENSIONS.contains(extension) == false) {
					logger.error "Unsupported file extension '$extension' for dependency '$name'"
					allExtensionsOk = false
				}
			} else {
				logger.error "No file extension found for dependency '$it.name'"
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