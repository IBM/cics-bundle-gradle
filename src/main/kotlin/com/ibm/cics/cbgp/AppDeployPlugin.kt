/*-
 * #%L
 * CICS Bundle Gradle Plugin
 * %%
 * Copyright (C) 2026 IBM Corp.
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
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.bundling.War

/**
 * Uploads a WAR file directly to a Liberty JVM server that has the cicsts:appDeploy-1.0 feature,
 * without building a CICS bundle. It is the Gradle equivalent of cics-appdeploy-maven-plugin.
 */
class AppDeployPlugin : Plugin<Project> {
	companion object {
		const val UPLOAD_TASK_NAME = "deployWarToLiberty"
		const val EXTENSION_NAME = "cicsAppDeploy"
	}

	override fun apply(project: Project) {

		// Create the cicsAppDeploy extension. The task reads it, so it must exist first.
		project.extensions.create(EXTENSION_NAME, LibertyWarUploadExtension::class.java)

		val uploadTaskProvider = project.tasks.register(UPLOAD_TASK_NAME, UploadWarToLibertyTask::class.java) {
			this.description = "Uploads a WAR file to a Liberty JVM server that has the cicsts:appDeploy-1.0 feature."
			this.group = "upload"
		}

		// By default, upload the WAR built by the war task, and run the war task first.
		project.plugins.withType(WarPlugin::class.java) {
			val warTask = project.tasks.named("war", War::class.java)
			uploadTaskProvider.configure {
				warFile.convention(warTask.flatMap { it.archiveFile })
				dependsOn(warTask)
			}
		}
	}
}
