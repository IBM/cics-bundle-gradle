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

import com.ibm.cics.bundle.parts.BundlePublisher
import com.ibm.cics.bundle.parts.BundlePublisher.PublishException
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class BuildBundleTask : AbstractBundleTask() {

	companion object {

		const val FAILED_DEPENDENCY_RESOLUTION = "Failed to resolve cicsBundle dependencies"
		const val MISSING_CONFIG = "Define '${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}' configuration with CICS bundle dependencies"
		const val MISSING_JVMSERVER = "Specify defaultjvmserver for build"
		const val NO_DEPENDENCIES_WARNING = "Warning, no external or project dependencies in '${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}' configuration"
		const val PLEASE_SPECIFY = "Please specify build configuration"
		const val UNSUPPORTED_EXTENSIONS_FOUND = "Unsupported file extensions for some dependencies, see earlier messages."

		val BUILD_CONFIG_EXCEPTION =
				PLEASE_SPECIFY + """\

			Example:
				 ${BundlePlugin.BUILD_EXTENSION_NAME} {
				   defaultjvmserver = 'EYUCMCIJ'
				} 
			""".trimIndent()
		val VALID_DEPENDENCY_FILE_EXTENSIONS = listOf("ear", "jar", "war")
	}

	@OutputDirectory
	val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

	@TaskAction
	fun buildCICSBundle() {
		logger.info("Task ${BundlePlugin.BUILD_TASK_NAME} (Gradle ${project.gradle.gradleVersion})")

		val buildExtension = project.extensions.getByName(BundlePlugin.BUILD_EXTENSION_NAME) as BuildExtension
		validateBuildExtension(buildExtension)
		this.defaultJvmserver = buildExtension.defaultjvmserver

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
		val bundlePublisher = initBundlePublisher(outputDirectory)
		processDependencies(config, bundlePublisher)

		// TODO Process CICS Bundle parts etc.

		try {
			bundlePublisher.publishResources()
		} catch (e: PublishException) {
			throw GradleException(e.message, e)
		}

	}

	private fun processDependencies(config: Configuration, bundlePublisher: BundlePublisher) {
		val resolved = validateResolvedFiles(config)
		if (resolved != null) {
			addResolvedFilesToBundle(config, resolved, bundlePublisher)
		}
	}

	private fun validateResolvedFiles(config: Configuration): ResolvedConfiguration? {
		val resolved = config.resolvedConfiguration
		if (resolved.hasError()) {
			throw GradleException(FAILED_DEPENDENCY_RESOLUTION)
		}

		if (resolved.files.isEmpty()) {
			logger.warn(NO_DEPENDENCIES_WARNING)
			return null
		}

		var allExtensionsOk = true
		resolved.files.forEach {
			if (it.extension.isNullOrEmpty()) {
				logger.error("No file extension found for dependency '${it.name}'")
				allExtensionsOk = false
			} else if (!VALID_DEPENDENCY_FILE_EXTENSIONS.contains(it.extension)) {
				logger.error("Unsupported file extension '${it.extension}' for dependency '${it.name}'")
				allExtensionsOk = false
			}
		}

		if (!allExtensionsOk) {
			throw GradleException(UNSUPPORTED_EXTENSIONS_FOUND)
		}
		return resolved
	}

	private fun addResolvedFilesToBundle(config: Configuration, resolved: ResolvedConfiguration, bundlePublisher: BundlePublisher) {

		val resolvedFiles = resolved.files.toTypedArray()
		val dependencies = config.dependencies.toTypedArray()

		// TODO - Depends on dependencies and resolved files being in the same order.
		for (i in resolvedFiles.indices) {
			val file = resolvedFiles[i]
			val name = getNameForDependency(dependencies[i])
			logger.lifecycle("Resolved '$name' to file: '$file'")
			// Already checked all extensions will be one of these
			when (file.extension) {
				"ear" -> addEar(file, name, bundlePublisher)
				"jar" -> addJar(file, name, bundlePublisher)
				"war" -> addWar(file, name, bundlePublisher)
			}
		}
		return
	}

	// TODO AddEar, addJar, addWar can be one method as only the binding differs (at the moment).
	private fun addEar(file: File, name: String, bundlePublisher: BundlePublisher) {
		val binding = EarbundlePartBinding()
		binding.name = name
		try {
			bundlePublisher.addResource(binding.toBundlePart(file, this))
		} catch (e: PublishException) {
			throw GradleException("Error adding bundle resource for artifact `$name` : ${e.message} ")
		}
	}

	private fun addJar(file: File, name: String, bundlePublisher: BundlePublisher) {
		val binding = OsgibundlePartBinding()
		binding.name = name
		try {
			bundlePublisher.addResource(binding.toBundlePart(file, this))
		} catch (e: PublishException) {
			throw GradleException("Error adding bundle resource for artifact `$name` : ${e.message} ")
		}
	}

	private fun addWar(file: File, name: String, bundlePublisher: BundlePublisher) {
		val binding = WarbundlePartBinding()
		binding.name = name
		try {
			bundlePublisher.addResource(binding.toBundlePart(file, this))
		} catch (e: PublishException) {
			throw GradleException("Error adding bundle resource for artifact `$name` : ${e.message} ")
		}
	}

	private fun getNameForDependency(dep: Dependency): String {
		if (dep is DefaultExternalModuleDependency) {
			return dep.name
		} else if (dep is DefaultProjectDependency) {
			return dep.dependencyProject.name
		} else {
			throw GradleException("Unexpected dependency type " + dep::class.java.toString() + " for dependency \$dep")
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