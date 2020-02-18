package com.ibm.cics.cbgp

import org.gradle.api.Action

open class BundleExtension {
	var defaultJVMServer = "MYJVMS"
	var cicsplex = ""
	var region = ""
	var bunddef = ""
	var csdgroup = ""
	var url = ""
	var username = ""
	var password = ""
	var insecure = false

	val overrides = mutableListOf<BundlePartOverride>()

	fun override(action: Action<in BundlePartOverride>) {
		val override = BundlePartOverride()
		action.execute(override)
		overrides.add(override)
	}
}