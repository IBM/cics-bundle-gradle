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

class OsgibundlePartBinding(file: File) : AbstractJavaBundlePartBinding(file) {

	var symbolicName: String? = ""
	var osgiVersion: String? = ""

	@Throws(GradleException::class)
	override fun applyDefaults(defaultJVMServer: String) {
		super.applyDefaults(defaultJVMServer)

		try {
			/**
			 * For other bundle parts, symbolic name can be anything, but osgi bundle parts must use the symbolic name
			 * that is in the manifest. This is mandatory so fail if not found in manifest.
			 */
			symbolicName = OsgiBundlePart.getBundleSymbolicName(file)
			if (symbolicName == null) {
				throw GradleException("No value found for mandatory OSGi bundle header 'Bundle-SymbolicName' in manifest file: $file")
			}

			/**
			 * OSGi version is optional, so use default value if not found in manifest.
			 */
			osgiVersion = OsgiBundlePart.getBundleVersion(file)
			if (osgiVersion == null) {
				osgiVersion = "0.0.0"
			}
		} catch (e: IOException) {
			throw GradleException("Error reading OSGi bundle headers from manifest file: $file", e)
		}
	}

	@Throws(GradleException::class)
	override fun toBundlePartImpl(): BundleResource {

		return OsgiBundlePart(
				name,
				symbolicName,
				osgiVersion,
				jvmserver,
				file
		)
	}
}