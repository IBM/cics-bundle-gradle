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

		// Define cicsBundle dependency configuration
		project.configurations.register(BUNDLE_DEPENDENCY_CONFIGURATION_NAME) {
			this.description = "Dependencies that constitute Java-based bundle parts that should be included in this CICS bundle."
			this.isVisible = false
		}

		// Define build task
		val buildTaskProvider = project.tasks.register(BUILD_TASK_NAME, BuildBundleTask::class.java) {
			this.description = "Builds a CICS bundle including all bundle parts."
			this.group = BasePlugin.BUILD_GROUP
		}

		// Define package task
		val packageTaskProvider = project.tasks.register(PACKAGE_TASK_NAME, PackageBundleTask::class.java) {
			this.description = "Packages a built CICS bundle into a zipped archive."
			this.group = BasePlugin.BUILD_GROUP
		}
		project.tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(packageTaskProvider)

		// Define deploy task
		val deployTaskProvider = project.tasks.register(DEPLOY_TASK_NAME, DeployBundleTask::class.java) {
			this.description = "Deploys a packaged CICS bundle to a CICS system."
			this.group = BasePlugin.UPLOAD_GROUP
		}

		// Configure tasks (projectsEvaluated ensures that this runs after values such as project.version are set)
		project.gradle.projectsEvaluated {

			// Set build directory to build/<name>-<version>, by default
			buildTaskProvider.get().outputDirectory.set(project.layout.buildDirectory.dir("${project.name}-${project.version}"))

			// Set resources directory to src/main/resources, by default
			val resources = project.layout.projectDirectory.dir(BuildBundleTask.RESOURCES_PATH)
			// Gradle will fail the build if we set a task input to a directory that doesn't exist
			if (resources.asFile.exists()) {
				buildTaskProvider.get().resourcesDirectory.set(resources)
			}

			// Wire output of build task to input of package task, by default
			packageTaskProvider.get().inputDirectory.set(buildTaskProvider.get().outputDirectory)

			// Set the output file to be the zip archive, by default
			packageTaskProvider.get().outputFile.set(packageTaskProvider.get().archivePath)

			// Wire output of package task to input of deploy task, by default
			deployTaskProvider.get().inputFile.set(packageTaskProvider.get().outputFile)

			// Never skip the deploy task, even if nothing has changed, because we can't know whether the bundle needs redeploying in CICS.
			deployTaskProvider.get().outputs.upToDateWhen { false }
		}

		// Add the bundle zip to the 'archives' configuration so it can be consumed by other projects
		val bundleArtifact = LazyPublishArtifact(packageTaskProvider)
		project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(bundleArtifact)
	}
}
