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
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskProvider

import javax.inject.Inject

class BundlePlugin implements Plugin<Project> {

	public static final String BUILD_TASK_NAME = 'buildCICSBundle'
	public static final String PACKAGE_TASK_NAME = 'packageCICSBundle'
	public static final String DEPLOY_TASK_NAME = 'deployCICSBundle'
	public static final String BUILD_EXTENSION_NAME = BUILD_TASK_NAME + 'Config'
	public static final String DEPLOY_EXTENSION_NAME = DEPLOY_TASK_NAME + 'Config'

	public static final String BUNDLE_DEPENDENCY_CONFIGURATION_NAME = "cicsBundle"

	private final ObjectFactory objectFactory

	@Inject
	BundlePlugin(ObjectFactory objectFactory) {
		this.objectFactory = objectFactory
	}

	void apply(Project project) {
		project.getPluginManager().apply(BasePlugin.class)
		project.extensions.create(BUILD_EXTENSION_NAME, BuildExtension)
		project.extensions.create(DEPLOY_EXTENSION_NAME, DeployExtension)

		project.getTasks().withType(BuildBundleTask).configureEach { bundleTask ->
			bundleTask.dependsOn {
				project.getConfigurations().getByName(BUNDLE_DEPENDENCY_CONFIGURATION_NAME)
			}
		}

		project.getTasks().withType(PackageBundleTask).configureEach { packageTask ->
			packageTask.from packageTask.inputDirectory
		}

		TaskProvider<BuildBundleTask> build = project.tasks.register(BUILD_TASK_NAME, BuildBundleTask, {
			it.setDescription("Generates a CICS bundle with all the bundle parts.")
			it.setGroup(BasePlugin.BUILD_GROUP)
		})

		TaskProvider<PackageBundleTask> pkg = project.tasks.register(PACKAGE_TASK_NAME, PackageBundleTask, {
			it.setDescription("Packages a CICS bundle into a zipped archive and includes external dependencies.")
			it.setGroup(BasePlugin.BUILD_GROUP)

		})
		project.tasks.register(DEPLOY_TASK_NAME, DeployBundleTask, {
			it.setDescription("Deploys a CICS bundle to a CICS system.")
			it.setGroup(BasePlugin.UPLOAD_GROUP)
		})

		pkg.configure {
			// Wire output of build task to input of package task, by default
			it.inputDirectory = build.flatMap { it.outputDirectory }
		}

		build.configure {
			// Define output for build task, by default
			it.outputDirectory = project.layout.buildDirectory.dir("$project.name-$project.version")
		}

		// Register output of archive task as the default artifact
		PublishArtifact bundleArtifact = new LazyPublishArtifact(pkg)
		project.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(bundleArtifact)

		configureConfigurations(project.getConfigurations())
	}

	private static void configureConfigurations(ConfigurationContainer configurationContainer) {
		configurationContainer.create(BUNDLE_DEPENDENCY_CONFIGURATION_NAME).setVisible(false).
				setDescription("Dependencies that constitute bundle parts that should be included in this CICS bundle.")
	}
}
