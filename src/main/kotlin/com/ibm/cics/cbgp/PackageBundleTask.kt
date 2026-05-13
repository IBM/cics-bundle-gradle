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
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.bundling.Zip
import javax.inject.Inject

@CacheableTask
abstract class PackageBundleTask @Inject constructor() : Zip() {

	/**
	 * Set the build directory as a task input. This will be linked to the output of the build task.
	 */
	@get:InputDirectory
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val inputDirectory: DirectoryProperty

    /**
     * Set the zip archive file as a task output. This will be linked to the input of the deploy task.
     */
    @get:OutputFile @Deprecated("Will be removed in v2.0.0", ReplaceWith("archiveFile"))
    abstract val outputFile: RegularFileProperty

	init {
		// Tell the task which directory to zip up
		from(inputDirectory)
		setMetadataCharset("UTF-8")
		outputFile.set(archiveFile)
	}
}