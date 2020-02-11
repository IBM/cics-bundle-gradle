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

abstract class AbstractBundlePartBinding(var file: File) {

	@Throws(GradleException::class)
	fun toBundlePart(defaultJVMServer: String): BundleResource {
		applyDefaults(defaultJVMServer)
		return toBundlePartImpl()
	}

	@Throws(GradleException::class)
	protected abstract fun applyDefaults(defaultJVMServer: String)

	@Throws(GradleException::class)
	protected abstract fun toBundlePartImpl(): BundleResource
}