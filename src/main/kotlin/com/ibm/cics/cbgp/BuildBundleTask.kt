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
import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.tasks.*
import java.io.File
import java.io.IOException
import java.nio.file.Files

open class BuildBundleTask : AbstractBundleTask() {

	companion object {

		const val RESOURCES_PATH = "src/main/resources"
		val VALID_DEPENDENCY_FILE_EXTENSIONS = listOf("ear", "jar", "war", "eba")

		const val FAILED_DEPENDENCY_RESOLUTION = "Failed to resolve '${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}' dependencies"
		const val NO_DEPENDENCIES_WARNING = "Warning, no external or project dependencies in '${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}' configuration"
		val UNSUPPORTED_EXTENSIONS_FOUND = "Unsupported file extensions for some dependencies, see earlier messages. Supported extensions are: $VALID_DEPENDENCY_FILE_EXTENSIONS."
	}

	@InputFiles
	val cicsBundleConfig: Configuration = project.configurations.getByName(BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME)

	/**
	 * This property will only get set if the src/main/resources directory exists, otherwise the build would fail.
	 */
	@InputDirectory
	@Optional
	val resourcesDirectory: DirectoryProperty = project.objects.directoryProperty()

	@OutputDirectory
	val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

	@TaskAction
	fun buildCICSBundle() {
		logger.info("Task ${BundlePlugin.BUILD_TASK_NAME} (Gradle ${project.gradle.gradleVersion})")

		val bundleExtension = project.extensions.getByName(BundlePlugin.BUNDLE_EXTENSION_NAME) as BundleExtension
		this.defaultJVMServer = bundleExtension.defaultJVMServer

		processCICSBundle(cicsBundleConfig)
	}

	private fun processCICSBundle(config: Configuration) {
		logger.info("Processing '${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}' configuration")
		val bundlePublisher = initBundlePublisher(outputDirectory)
		processDependencies(config, bundlePublisher)
		addStaticResourcesToBundle(bundlePublisher)

		val buildDirectory = project.layout.buildDirectory.get().asFile
		if (buildDirectory.exists()) {
			logger.debug("Deleting $buildDirectory")
			try {
				FileUtils.deleteDirectory(buildDirectory)
			} catch (e: IOException) {
				throw GradleException("Unable to delete CICS bundle output directory $buildDirectory", e)
			}
		}

		try {
			bundlePublisher.publishResources()
			bundlePublisher.publishDynamicResources()
		} catch (e: PublishException) {
			throw GradleException(e.message as String, e)
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
				"eba" -> addEba(file, name, bundlePublisher)
			}
		}
		return
	}

	private fun addEar(file: File, name: String, bundlePublisher: BundlePublisher) {
		val binding = EarbundlePartBinding()
		addJavaBundlePartBinding(file, name, binding, bundlePublisher)
	}

	private fun addJar(file: File, name: String, bundlePublisher: BundlePublisher) {
		val binding = OsgibundlePartBinding()
		addJavaBundlePartBinding(file, name, binding, bundlePublisher)
	}

	private fun addWar(file: File, name: String, bundlePublisher: BundlePublisher) {
		val binding = WarbundlePartBinding()
		addJavaBundlePartBinding(file, name, binding, bundlePublisher)
	}

	private fun addEba(file: File, name: String, bundlePublisher: BundlePublisher) {
		val binding = EbabundlePartBinding()
		addJavaBundlePartBinding(file, name, binding, bundlePublisher)
	}

	private fun addJavaBundlePartBinding(file: File, name: String, binding: AbstractNameableJavaBundlePartBinding, bundlePublisher: BundlePublisher) {
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
			throw GradleException("Unexpected dependency type ${dep::class.java} for dependency \$dep")
		}
	}

	private fun addStaticResourcesToBundle(bundlePublisher: BundlePublisher) {

		if (resourcesDirectory.isPresent) {
			logger.lifecycle("Adding bundle parts from '$RESOURCES_PATH'")
			val resourcesDirectoryPath = resourcesDirectory.get().asFile.toPath()
			if (Files.isDirectory(resourcesDirectoryPath)) {
				try {
					File(resourcesDirectoryPath.toString())
							.walk(FileWalkDirection.TOP_DOWN)
							.filter { it.isFile }
							.forEach {
								try {
									logger.lifecycle("Adding bundle part '${it.name}'")
									bundlePublisher.addStaticResource(resourcesDirectoryPath.relativize(it.toPath())) {
										Files.newInputStream(it.toPath())
									}
								} catch (e: PublishException) {
									throw GradleException("Failure adding bundle part '${it.name}' : ${e.message}", e)
								}
							}
				} catch (e: IOException) {
					throw GradleException("Failure adding bundle parts", e)
				}
			} else {
				throw GradleException("Resources folder path '$resourcesDirectoryPath' is not a directory")
			}
		} else {
			logger.info("No resources folder '$RESOURCES_PATH' to search for bundle parts")
		}
	}
}
