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
import org.gradle.api.artifacts.ConfigurationContainer
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
		project.pluginManager.apply(BasePlugin::class.java)
		project.extensions.create(BUNDLE_EXTENSION_NAME, BundleExtension::class.java)

		project.tasks.withType(BuildBundleTask::class.java).configureEach {
			it.dependsOn(
				project.configurations.getByName(BUNDLE_DEPENDENCY_CONFIGURATION_NAME)
			)
		}

		project.tasks.withType(PackageBundleTask::class.java).configureEach { packageTask ->
			packageTask.from(packageTask.inputDirectory)
		}

		val build = project.tasks.register(BUILD_TASK_NAME, BuildBundleTask::class.java) {
			it.description = "Generates a CICS bundle with all the bundle parts."
			it.group = BasePlugin.BUILD_GROUP
		}

		val pkg = project.tasks.register(PACKAGE_TASK_NAME, PackageBundleTask::class.java) {
			it.description = "Packages a CICS bundle into a zipped archive and includes external dependencies."
			it.group = BasePlugin.BUILD_GROUP
		}

		val deploy = project.tasks.register(DEPLOY_TASK_NAME, DeployBundleTask::class.java) {
			it.description = "Deploys a CICS bundle to a CICS system."
			it.group = BasePlugin.UPLOAD_GROUP
		}

		pkg.configure { packageBundleTask ->
			// Wire output of build task to input of package task, by default
			packageBundleTask.inputDirectory.set(build.flatMap { buildBundleTask -> buildBundleTask.outputDirectory })
		}

		deploy.configure { deployBundleTask ->
			// Wire output of package task to input of deploy task, by default
			deployBundleTask.inputFile.set(pkg.flatMap { packageBundleTask -> packageBundleTask.outputFile })
		}

		build.configure {
			// Define output for build task, by default
			it.outputDirectory.set(project.layout.buildDirectory.dir("${project.name}-${project.version}"))
		}

		pkg.configure {
			// Define output for package task, by default
			it.outputFile.set(pkg.get().archivePath)
		}

		// Register output of archive task as the default artifact
		val bundleArtifact = LazyPublishArtifact(pkg)
		project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(bundleArtifact)

		configureConfigurations(project.configurations)
	}

	private fun configureConfigurations(configurationContainer: ConfigurationContainer) {
		val configuration = configurationContainer.create(BUNDLE_DEPENDENCY_CONFIGURATION_NAME)
		configuration.isVisible = false
		configuration.description = "Dependencies that constitute bundle parts that should be included in this CICS bundle."
	}
}
