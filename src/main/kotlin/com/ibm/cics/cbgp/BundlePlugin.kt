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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.plugins.BasePlugin

class BundlePlugin : Plugin<Project> {
	companion object {
		const val BUILD_TASK_NAME = "buildCICSBundle"
		const val PACKAGE_TASK_NAME = "packageCICSBundle"
		const val DEPLOY_TASK_NAME = "deployCICSBundle"
		const val BUNDLE_EXTENSION_NAME = "cicsBundle"
		const val BUNDLE_DEPENDENCY_CONFIGURATION_NAME = "cicsBundle"
	}

	override fun apply(project: Project) {

		// Apply the Base Plugin
		project.pluginManager.apply(BasePlugin::class.java)

		// Create cicsBundle extension
		project.extensions.create(BUNDLE_EXTENSION_NAME, BundleExtension::class.java)

		// Define and configure cicsBundle dependency configuration
		project.configurations.register(BUNDLE_DEPENDENCY_CONFIGURATION_NAME) { cicsBundleConfig ->
			cicsBundleConfig.description = "Dependencies that constitute bundle parts that should be included in this CICS bundle."
			cicsBundleConfig.isVisible = false
		}

		// Define and configure build task
		val buildTaskProvider = project.tasks.register(BUILD_TASK_NAME, BuildBundleTask::class.java) { buildTask ->
			buildTask.description = "Generates a CICS bundle with all the bundle parts."
			buildTask.group = BasePlugin.BUILD_GROUP

			// Set resources directory to src/main/resources, by default
			val resources = project.layout.projectDirectory.dir(BuildBundleTask.RESOURCES_PATH)
			// Gradle will fail the build if we set a task input to a directory that doesn't exist
			if (resources.asFile.exists()) {
				buildTask.resourcesDirectory.set(resources)
			}

			// Set build directory to build/<name>-<version>, by default
			buildTask.outputDirectory.set(project.layout.buildDirectory.dir("${project.name}-${project.version}"))
		}

		// Define and configure package task
		val packageTaskProvider = project.tasks.register(PACKAGE_TASK_NAME, PackageBundleTask::class.java) { packageTask ->
			packageTask.description = "Packages a CICS bundle into a zipped archive and includes external dependencies."
			packageTask.group = BasePlugin.BUILD_GROUP

			// Wire output of build task to input of package task, by default
			packageTask.inputDirectory.set(buildTaskProvider.get().outputDirectory)

			// Set the output file to be the zip archive, by default
			packageTask.outputFile.set(packageTask.archivePath)
		}

		// Define and configure deploy task
		val deployTaskProvider = project.tasks.register(DEPLOY_TASK_NAME, DeployBundleTask::class.java) { deployTask ->
			deployTask.description = "Deploys a CICS bundle to a CICS system."
			deployTask.group = BasePlugin.UPLOAD_GROUP

			// Wire output of package task to input of deploy task, by default
			deployTask.inputFile.set(packageTaskProvider.get().outputFile)
		}

		// Register output of package task as the default artifact
		val bundleArtifact = LazyPublishArtifact(packageTaskProvider)
		project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(bundleArtifact)
	}
}
