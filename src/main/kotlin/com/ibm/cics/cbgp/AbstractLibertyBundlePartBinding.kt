/*
 * #%L
 * CICS Bundle Gradle Plugin
 * %%
 * Copyright (C) 2019, 2023 IBM Corp.
 * %%
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * #L%
 */
package com.ibm.cics.cbgp

import java.io.File

abstract class AbstractLibertyBundlePartBinding() : AbstractJavaBundlePartBinding() {
	/** Binds to the addCICSAllAuth bundle attribute. */
	var addCICSAllAuthenticatedRole: Boolean = true

	/** Binds to the appConfigFile bundle attribute. */
	var libertyAppConfigFile: File? = null

	override fun extraConfigAsString(): String {
		val map = LinkedHashMap<String, String>()
		if (name.isNotEmpty()) map["name"] = name
		if (jvmserver.isNotEmpty()) map["jvmserver"] = jvmserver
		if (versionRange.isNotEmpty()) map["versionRange"] = versionRange
		map["addCICSAllAuthenticatedRole"] = addCICSAllAuthenticatedRole.toString()
		libertyAppConfigFile?.let { map["libertyAppConfigFile"] = it.path }
		return map.toString()
	}
}
