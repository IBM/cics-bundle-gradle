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

import org.gradle.api.file.DirectoryProperty

import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip

open class PackageBundleTask : Zip() {
	companion object {
		const val BUNDLE_EXTENSION = "zip"
	}

	@InputDirectory
	var inputDirectory: DirectoryProperty = project.objects.directoryProperty()

	init {
		extension = BUNDLE_EXTENSION
		metadataCharset = "UTF-8"
	}

	@TaskAction
	fun packageCICSBundle() {

	}
}