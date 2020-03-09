package com.ibm.cics.cbgp

import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.util.ConfigureUtil

/**
 * Applies extra configuration to Java-based bundle parts. E.g. overrides for name, jvmserver, extension.
 */
open class ExtraConfigExtension() {

	val bundlePartsWithExtraConfig = mutableListOf<AbstractJavaBundlePartBinding>()

	fun cicsBundleOsgi(extraConfig: Any) {
		configureBundlePart(extraConfig, OsgiBundlePartBinding())
	}

	fun cicsBundleWar(extraConfig: Any) {
		configureBundlePart(extraConfig, WarBundlePartBinding())
	}

	fun cicsBundleEar(extraConfig: Any) {
		configureBundlePart(extraConfig, EarBundlePartBinding())
	}

	fun cicsBundleEba(extraConfig: Any) {
		configureBundlePart(extraConfig, EbaBundlePartBinding())
	}

	private fun configureBundlePart(extraConfig: Any, bundlePart: AbstractJavaBundlePartBinding) {
		when (extraConfig) {
			is Closure<*> -> {
				ConfigureUtil.configure(extraConfig, bundlePart)
			}
			is Map<*, *> -> {
				ConfigureUtil.configureByMap(extraConfig, bundlePart)
			}
			else -> {
				throw GradleException("'$extraConfig' cannot be used to configure a bundle part. Only Closure or Map are supported.")
			}
		}
		bundlePartsWithExtraConfig.add(bundlePart)
	}
}