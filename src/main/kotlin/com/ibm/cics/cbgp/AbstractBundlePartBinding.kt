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
import org.gradle.api.GradleException
import java.io.File

abstract class AbstractBundlePartBinding {

	var cacheFile: File? = null

	@Throws(GradleException::class)
	fun toBundlePart(cacheFile: File?, defaultJVMServer: String): BundleResource {
		applyDefaults(cacheFile, defaultJVMServer)
		return toBundlePartImpl(cacheFile)
	}

	@Throws(GradleException::class)
	protected abstract fun applyDefaults(file: File?, defaultJVMServer: String)

	@Throws(GradleException::class)
	protected abstract fun toBundlePartImpl(file: File?): BundleResource
}