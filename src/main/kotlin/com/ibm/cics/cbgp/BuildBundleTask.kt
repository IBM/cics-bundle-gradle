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
package com.ibm.cics.cbgp

import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class BuildBundleTask : AbstractBundleTask() {

	companion object {
		const val MISSING_JVMSERVER = "Specify defaultjvmserver for build"
		const val PLEASE_SPECIFY = "Please specify build configuration"
		val BUILD_CONFIG_EXCEPTION =
			PLEASE_SPECIFY + """\

			Example:
				 ${BundlePlugin.BUILD_EXTENSION_NAME} {
				   defaultjvmserver = 'EYUCMCIJ'
				} 
			""".trimIndent()
		val MISSING_CONFIG = "Define '${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}' configuration with CICS bundle dependencies"
		const val UNSUPPORTED_EXTENSIONS_FOUND = "Unsupported file extensions for some dependencies, see earlier messages."
		val VALID_DEPENDENCY_FILE_EXTENSIONS = listOf("ear", "jar", "war")
	}

	@OutputDirectory
	val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

	private val resolvedDependencies: MutableList<File> = mutableListOf()

	@TaskAction
	fun buildCICSBundle() {
		logger.info("Task ${BundlePlugin.BUILD_TASK_NAME} (Gradle ${project.gradle.gradleVersion})")

		val buildExtension = project.extensions.getByName(BundlePlugin.BUILD_EXTENSION_NAME) as BuildExtension
		validateBuildExtension(buildExtension)

		// Find & process the configuration
		val foundConfig = project.configurations.find {
			if (it.name == BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME) {
				processCICSBundle(it)
				true
			} else {
				false
			}
		} != null

		if (!foundConfig) {
			throw GradleException(MISSING_CONFIG)
		}
	}

	private fun processCICSBundle(config: Configuration) {
		logger.info("processing '${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}' configuration")
		val resolved = config.resolvedConfiguration
		if (resolved.hasError()) {
			throw GradleException("Failed to resolve cicsBundle dependencies")
		}

		if (resolved.files.isEmpty()) {
			logger.warn("Warning, no external or project dependencies in '${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}' configuration")
			return
		}

		resolved.files.forEach {
			logger.lifecycle("Resolved file: $it")
			resolvedDependencies.add(it)
		}

		checkResolvedFileExtensions()
	}

	private fun checkResolvedFileExtensions() {
		var allExtensionsOk = true
		resolvedDependencies.forEach {
			val name = it.name
			val dotpos = name.lastIndexOf('.')
			if (dotpos > -1) {
				val extension = name.substring(dotpos + 1)
				if (!VALID_DEPENDENCY_FILE_EXTENSIONS.contains(extension)) {
					logger.error("Unsupported file extension '$extension' for dependency '$name'")
					allExtensionsOk = false
				}
			} else {
				logger.error("No file extension found for dependency '${it.name}'")
				allExtensionsOk = false
			}
		}
		if (!allExtensionsOk) {
			throw GradleException(UNSUPPORTED_EXTENSIONS_FOUND)
		}
	}

	private fun validateBuildExtension(buildExtension: BuildExtension) {
		var blockValid = true

		// Validate block items exist, no check on content
		if (buildExtension.defaultjvmserver.isEmpty()) {
			logger.error(MISSING_JVMSERVER)
			blockValid = false
		}

		// Throw exception if anything is wrong in the extension block
		if (!blockValid) {
			throw GradleException(BUILD_CONFIG_EXCEPTION)
		}
	}


}