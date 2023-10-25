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
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.BasePlugin
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.extra

class BundlePlugin : Plugin<Project> {
	companion object {
		const val BUILD_TASK_NAME = "buildCICSBundle"
		const val PACKAGE_TASK_NAME = "packageCICSBundle"
		const val DEPLOY_TASK_NAME = "deployCICSBundle"
		const val BUNDLE_EXTENSION_NAME = "cicsBundle"
		const val BUNDLE_DEPENDENCY_CONFIGURATION_NAME = "cicsBundlePart"
		const val EXTRA_CONFIG_EXTENSION_NAME = "extraConfig"
		const val OSGI_METHOD_NAME = "cicsBundleOsgi"
		const val WAR_METHOD_NAME = "cicsBundleWar"
		const val EAR_METHOD_NAME = "cicsBundleEar"
		const val EBA_METHOD_NAME = "cicsBundleEba"
		const val CICS_BUNDLE_USAGE = "cicsBundle"
	}

	override fun apply(project: Project) {

		// Apply the Base Plugin
		project.pluginManager.apply(BasePlugin::class.java)

		// Create cicsBundle extension
		project.extensions.create(BUNDLE_EXTENSION_NAME, BundleExtension::class.java)

		// Create extra config extension and delegate some DSL methods to it. This lets us put these methods at the project level, rather than have to have them nested under the extension.
		val extraConfigExtension = project.extensions.create(EXTRA_CONFIG_EXTENSION_NAME, ExtraConfigExtension::class.java)
		project.extra.set(OSGI_METHOD_NAME, closureOf<Any> { extraConfigExtension.cicsBundleOsgi(this) })
		project.extra.set(WAR_METHOD_NAME, closureOf<Any> { extraConfigExtension.cicsBundleWar(this) })
		project.extra.set(EAR_METHOD_NAME, closureOf<Any> { extraConfigExtension.cicsBundleEar(this) })
		project.extra.set(EBA_METHOD_NAME, closureOf<Any> { extraConfigExtension.cicsBundleEba(this) })

		// Define cicsBundlePart dependency configuration
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


		project.configurations.create("cicsBundle") {
			isCanBeConsumed = true
			isCanBeResolved = false
			val objects = project.objects
			attributes {
				attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
				attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, BundlePlugin.CICS_BUNDLE_USAGE))
			}
		}

		project.artifacts.add("cicsBundle", packageTaskProvider)

		// Define deploy task
		val deployTaskProvider = project.tasks.register(DEPLOY_TASK_NAME, DeployBundleTask::class.java) {
			this.description = "Deploys a packaged CICS bundle to a CICS system."
			this.group = "upload"
		}

		// Configure tasks (projectsEvaluated ensures that this runs after values such as project.version are set)
		project.gradle.projectsEvaluated {

			// Set build directory to build/<name>-<version>, by default
			buildTaskProvider.configure {
				outputDirectory.set(project.layout.buildDirectory.dir("${project.name}-${project.version}"))
			}

			// Set resources directory to configured location, by default
			val resources = project.layout.projectDirectory.dir(buildTaskProvider.get().bundlePartsDirectory)
			// Gradle will fail the build if we set a task input to a directory that doesn't exist
			if (resources.asFile.exists()) {
				buildTaskProvider.get().resourcesDirectory.set(resources)
			}

			packageTaskProvider.configure {
				// Wire output of build task to input of package task, by default
				inputDirectory.set(buildTaskProvider.flatMap { it.outputDirectory })
			}

			deployTaskProvider.configure {
				// Wire output of package task to input of deploy task, by default
				inputFile.set(packageTaskProvider.flatMap { it.archiveFile })

				// Never skip the deploy task, even if nothing has changed, because we can't know whether the bundle needs redeploying in CICS.
				outputs.upToDateWhen { false }
			}
		}
	}
}
