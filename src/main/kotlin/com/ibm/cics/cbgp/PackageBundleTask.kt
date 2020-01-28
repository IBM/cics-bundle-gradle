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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.bundling.Zip

open class PackageBundleTask : Zip() {

	/**
	 * Set the build directory as a task input. This will be linked to the output of the build task.
	 */
	@InputDirectory
	var inputDirectory: DirectoryProperty = project.objects.directoryProperty()

	/**
	 * Set the zip archive file as a task output. This will be linked to the input of the deploy task.
	 */
	@OutputFile
	var outputFile: RegularFileProperty = project.objects.fileProperty()

	init {
		// Tell the task which directory to zip up
		from(inputDirectory)
		metadataCharset = "UTF-8"
	}
}