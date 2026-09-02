/*
 * #%L
 * CICS Bundle Gradle Plugin
 * %%
 * Copyright (C) 2019, 2026 IBM Corp.
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
import com.ibm.cics.bundle.parts.EarBundlePart

class EarBundlePartBinding() : AbstractLibertyBundlePartBinding() {

	override fun toBundlePart(): BundleResource {
		return EarBundlePart(
				name,
				jvmserver,
				addCICSAllAuthenticatedRole,
				libertyAppConfigFile,
				file,
		)
	}
}
