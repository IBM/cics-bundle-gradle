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

import org.gradle.api.GradleException
import java.io.File

abstract class AbstractNameableJavaBundlePartBinding : AbstractJavaBundlePartBinding() {
	var name : String = ""

	@Throws(GradleException::class)
	override fun applyDefaults(file: File?, defaultJVMServer: String) {
		if (name.isNullOrEmpty()) {
			if (file != null) {
				name = file.nameWithoutExtension
			} else {
				throw GradleException("Unable to set part name as filename not provided")
			}
		}
		super.applyDefaults(file, defaultJVMServer)
	}
}