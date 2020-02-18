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

		const val RESOURCES_PATH = "src/main/resources"
		val VALID_DEPENDENCY_FILE_EXTENSIONS = listOf("ear", "jar", "war", "eba")
	}

	/**
	 * Set parameters from the cicsBundle extension as task inputs.
	 */
	@Internal
	val bundleExtension = project.extensions.getByName(BundlePlugin.BUNDLE_EXTENSION_NAME) as BundleExtension
	@Input
	val defaultJVMServer = bundleExtension.build.defaultJVMServer

	/**
	 * Set the cicsBundle dependency configuration as a task input.
	 */
	@InputFiles
	val cicsBundleConfig: Configuration = project.configurations.getByName(BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME)

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
			logger.debug("Deleting $name output directory: $outputDirectoryFile")
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
				versionNumber.patch
		)
		return bundlePublisher
	}

	private fun getProjectVersionNumber(): VersionNumber {
		val pv = project.version
		if (pv is String) {
			val versionNumber = VersionNumber.parse(pv.toString())
			if (!VersionNumber.UNKNOWN.equals(versionNumber)) {
				return versionNumber
			}
		}
		throw GradleException("Project version number '$pv' could not be parsed into MAJOR.MINOR.MICRO.PATCH format")
	}

	private fun addJavaBundlePartsToBundle(bundlePublisher: BundlePublisher) {
		logger.info("Adding Java-based bundle parts from '${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}' dependency configuration")
		val resolved = cicsBundleConfig.resolvedConfiguration
		if (resolved.hasError()) {
			throw GradleException("Failed to resolve Java-based bundle parts from '${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}' dependency configuration")
		}

		if (resolved.files.isEmpty()) {
			logger.info("No Java-based bundle parts found in '${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}' dependency configuration")
			return
		}

		resolved.files.toTypedArray().forEach { file ->
			logger.lifecycle("Adding Java-based bundle part: '$file'")
			val binding: AbstractJavaBundlePartBinding
			when (file.extension) {
				"jar" -> binding = OsgibundlePartBinding(file)
				"war" -> binding = WarbundlePartBinding(file)
				"ear" -> binding = EarbundlePartBinding(file)
				"eba" -> binding = EbabundlePartBinding(file)
				else -> throw GradleException("Unsupported file extension '${file.extension}' for Java-based bundle part '${file.name}'. Supported extensions are: $VALID_DEPENDENCY_FILE_EXTENSIONS.")
			}
			try {
				bundlePublisher.addResource(binding.toBundlePart(defaultJVMServer))
			} catch (e: PublishException) {
				throw GradleException("Failure adding Java-based bundle part '${binding.name}' : ${e.message}", e)
			}
		}
		return
	}

	private fun addNonJavaBundlePartsToBundle(bundlePublisher: BundlePublisher) {
		logger.info("Adding non-Java-based bundle parts from '$RESOURCES_PATH'")
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
			logger.info("No non-Java-based bundle parts to add, because resources directory '$RESOURCES_PATH' does not exist")
		}
	}
}
