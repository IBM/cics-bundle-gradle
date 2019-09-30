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

import com.ibm.cics.bundle.parts.BundleResource
import com.ibm.cics.bundle.parts.OsgiBundlePart
import org.gradle.api.GradleException
import java.io.File
import java.io.IOException

class OsgibundlePartBinding : AbstractJavaBundlePartBinding() {

	// TODO - Why doesn't this class extend AbstractNameableJavaPartBinding?
	var name : String = ""

	@Throws(GradleException::class)
	override fun toBundlePartImpl(file: File?): BundleResource {
		var osgiVersion: String?
		try {
			osgiVersion = OsgiBundlePart.getBundleVersion(file)
		} catch (e: IOException) {
			throw GradleException("Error reading OSGi bundle version", e)
		}

		if (osgiVersion == null) {
			throw GradleException("TODO Parse version from file name or pass in from cicsBundle config dependency")
		}

		return OsgiBundlePart(
				name,
				osgiVersion,
				jvmserver,
				file
		)
	}
}