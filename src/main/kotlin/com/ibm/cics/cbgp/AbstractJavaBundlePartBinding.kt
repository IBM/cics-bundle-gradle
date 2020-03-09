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

abstract class AbstractJavaBundlePartBinding() : AbstractBundlePartBinding() {

	companion object {
		val JVMSERVER_EXCEPTION = """
			Please specify defaultJVMServer in build.gradle.
			Example:
				${BundlePlugin.BUNDLE_EXTENSION_NAME} {
					build {
						defaultJVMServer = 'DFHWLP'
					}
				}
			""".trimIndent()
	}

	var name: String = ""
	var jvmserver: String = ""

	@Throws(GradleException::class)
	override fun applyDefaults(defaultJVMServer: String) {
		if (name.isEmpty()) {
			name = file.nameWithoutExtension
		}
		if (jvmserver.isEmpty()) {
			jvmserver = defaultJVMServer
			if (jvmserver.isEmpty()) {
				throw GradleException(JVMSERVER_EXCEPTION)
			}
		}
	}

	fun extraConfigAsString(): String {
		val map = LinkedHashMap<String, String>()
		if (name.isNotEmpty()) {
			map["name"] = name
		}
		if (jvmserver.isNotEmpty()) {
			map["jvmserver"] = jvmserver
		}
		return map.toString()
	}
}
