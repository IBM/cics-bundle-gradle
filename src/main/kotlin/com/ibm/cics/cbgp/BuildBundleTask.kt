/*
 * #%L
 * CICS Bundle Gradle Plugin
 * %%
 * Copyright (C) 2019, 2023 IBM Corp.
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
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.gradle.util.VersionNumber
import java.io.File
import java.io.IOException
import java.nio.file.Files

open class BuildBundleTask : DefaultTask() {

	companion object {
		val VALID_DEPENDENCY_FILE_EXTENSIONS = listOf("ear", "jar", "war", "eba")
	}

	/**
	 * Set parameters from the cicsBundle extension as task inputs.
	 */
	@Internal
	val bundleExtension = project.extensions.getByName(BundlePlugin.BUNDLE_EXTENSION_NAME) as BundleExtension
	@Input
	val defaultJVMServer = bundleExtension.build.defaultJVMServer
	@Input
	val bundlePartsDirectory = "src/main/${bundleExtension.build.bundlePartsDirectory}"

	/**
	 * Set parameters from the extraConfig extension as task inputs.
	 */
	@Internal
	val extraConfigExtension = project.extensions.getByName(BundlePlugin.EXTRA_CONFIG_EXTENSION_NAME) as ExtraConfigExtension
	@Input
	val unused = extraConfigExtension.bundlePartsWithExtraConfig.hashCode()

	/**
	 * Set the cicsBundlePart dependency configuration as a task input.
	 */
	@InputFiles
	val cicsBundlePartConfig: Configuration = project.configurations.getByName(BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME)

	/**
	 * Set the resources directory as an optional task input.
	 */
	@InputDirectory
	@Optional
	val resourcesDirectory: DirectoryProperty = project.objects.directoryProperty()

	/**
	 * Set the build output directory as a task output. This will be linked to the input of the package task.
	 */
	@OutputDirectory
	val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

	@TaskAction
	fun buildCICSBundle() {
		logger.info("Task ${BundlePlugin.BUILD_TASK_NAME} (Gradle ${project.gradle.gradleVersion})")

		// Delete existing output directory
		val outputDirectoryFile = outputDirectory.get().asFile
		if (outputDirectoryFile.exists()) {
			logger.info("Deleting $name output directory: $outputDirectoryFile")
			try {
				FileUtils.deleteDirectory(outputDirectoryFile)
			} catch (e: IOException) {
				throw GradleException("Unable to delete $name output directory: $outputDirectoryFile", e)
			}
		}

		logger.info("Adding bundle parts to the bundle")
		val bundlePublisher = initBundlePublisher()

		addJavaBundlePartsToBundle(bundlePublisher)
		addNonJavaBundlePartsToBundle(bundlePublisher)

		bundlePublisher.publishResources()
		bundlePublisher.publishDynamicResources()
	}

	private fun initBundlePublisher(): BundlePublisher {
		val outputDirectoryPath = outputDirectory.asFile.get().toPath()
		val versionNumber = getProjectVersionNumber()
		val bundlePublisher = BundlePublisher(
				outputDirectoryPath,
				project.name,
				versionNumber.major,
				versionNumber.minor,
				versionNumber.micro,
		)
		return bundlePublisher
	}

	private fun getProjectVersionNumber(): VersionNumber {
		val pv = project.version
		val versionNumber = VersionNumber.parse(pv.toString())
		if (!VersionNumber.UNKNOWN.equals(versionNumber)) {
			return versionNumber
		}
		throw GradleException("Project version number '$pv' could not be parsed into MAJOR.MINOR.MICRO.PATCH format")
	}

	private fun addJavaBundlePartsToBundle(bundlePublisher: BundlePublisher) {
		logger.lifecycle("Adding Java-based bundle parts from '${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}' dependency configuration")

		val resolved = cicsBundlePartConfig.resolvedConfiguration
		if (resolved.hasError()) {
			throw GradleException("Failed to resolve Java-based bundle parts from '${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}' dependency configuration")
		}

		if (resolved.files.isEmpty()) {
			logger.info("No Java-based bundle parts found in '${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}' dependency configuration")
			return
		}

		val extraConfigMap = createExtraConfigMap()

		resolved.files.toTypedArray().forEach { file ->
			logger.lifecycle("Adding Java-based bundle part: '$file'")

			// If extra config has been provided for this bundle part then use it
			var binding: AbstractJavaBundlePartBinding? = extraConfigMap[file.name]
			if (binding == null) {
				when (file.extension) {
					"jar" -> binding = OsgiBundlePartBinding()
					"war" -> binding = WarBundlePartBinding()
					"ear" -> binding = EarBundlePartBinding()
					"eba" -> binding = EbaBundlePartBinding()
					else -> throw GradleException("Unsupported file extension '${file.extension}' for Java-based bundle part '${file.name}'. Supported extensions are: $VALID_DEPENDENCY_FILE_EXTENSIONS.")
				}
			}
			binding.file = file
			binding.applyDefaults(defaultJVMServer)

			try {
				bundlePublisher.addResource(binding.toBundlePart())
			} catch (e: PublishException) {
				throw GradleException("Failure adding Java-based bundle part '${binding.name}' : ${e.message}", e)
			}
		}
		return
	}

	/**
	 * Create a map of bundle part filename to a bundle part binding containing it's extra configuration.
	 */
	private fun createExtraConfigMap(): Map<String, AbstractJavaBundlePartBinding> {
		val extraConfigMap = HashMap<String, AbstractJavaBundlePartBinding>()
		extraConfigExtension.bundlePartsWithExtraConfig.forEach { bundlePartWithExtraConfig ->
			// Get the resolved filenames for the dependency by assigning it to a temporary configuration
			val temp: Configuration = project.configurations.create("cicsBundleTemp")
			temp.dependencies.add(bundlePartWithExtraConfig.dependency)
			temp.resolvedConfiguration.files.toTypedArray().forEach { file ->
				logger.lifecycle("Extra configuration found for Java-based bundle part: '${file.name}': ${bundlePartWithExtraConfig.extraConfigAsString()}")
				extraConfigMap[file.name] = bundlePartWithExtraConfig
			}
			project.configurations.remove(temp)
		}
		return extraConfigMap
	}

	private fun addNonJavaBundlePartsToBundle(bundlePublisher: BundlePublisher) {
		logger.lifecycle("Adding non-Java-based bundle parts from '$bundlePartsDirectory'")
		if (resourcesDirectory.isPresent) {
			val resourcesDirectoryPath = resourcesDirectory.get().asFile.toPath()
			try {
				File(resourcesDirectoryPath.toString())
						.walk(FileWalkDirection.TOP_DOWN)
						.filter { it.isFile }
						.forEach { file ->
							try {
								logger.lifecycle("Adding non-Java-based bundle part: '${file.name}'")
								bundlePublisher.addStaticResource(resourcesDirectoryPath.relativize(file.toPath())) {
									Files.newInputStream(file.toPath())
								}
							} catch (e: PublishException) {
								throw GradleException("Failure adding non-Java-based bundle part: '${file.name}' : ${e.message}", e)
							}
						}
			} catch (e: IOException) {
				throw GradleException("Failure adding non-Java-based bundle parts", e)
			}
		} else {
			logger.info("No non-Java-based bundle parts to add, because resources directory '$bundlePartsDirectory' does not exist")
		}
	}
}
