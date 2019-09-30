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
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.util.VersionNumber

open class AbstractBundleTask : DefaultTask() {
	companion object {
		const val BAD_VERSION_NUMBER = "Bad project version number"
	}

	@Input
	var defaultJvmserver = ""

	protected fun initBundlePublisher(outputDirectory: DirectoryProperty): BundlePublisher {
		val outputDirAsFile = outputDirectory.asFile.get()
		val versionNumber = getProjectVersionNumber()
		val bundlePublisher = BundlePublisher(
				outputDirAsFile.toPath(),
				project.name,
				versionNumber.major,
				versionNumber.minor,
				versionNumber.micro,
				versionNumber.patch
		)
		// TODO

		return bundlePublisher
	}

	private fun getProjectVersionNumber(): VersionNumber {
		val pv = project.version
		if (pv is String) {
			val versionNumber = VersionNumber.parse(pv.toString())
			if (versionNumber != VersionNumber.UNKNOWN) {
				return versionNumber
			}
		}
		throw GradleException(BAD_VERSION_NUMBER)
	}


}