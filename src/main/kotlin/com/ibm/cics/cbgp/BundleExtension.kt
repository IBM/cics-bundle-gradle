package com.ibm.cics.cbgp

import org.gradle.api.Action

open class BundleExtension {

	val build: BundleBuildExtension = BundleBuildExtension()
	val deploy: BundleDeployExtension = BundleDeployExtension()

	fun build(action: Action<in BundleBuildExtension>) {
		action.execute(build)
	}

	fun deploy(action: Action<in BundleDeployExtension>) {
		action.execute(deploy)
	}
}