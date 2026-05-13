package com.ibm.cics.cbgp

import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Dependency

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
				// Use Groovy's Closure delegation instead of deprecated ConfigureUtil
				val clonedClosure = extraConfig.clone() as Closure<*>
				clonedClosure.delegate = bundlePart
				clonedClosure.resolveStrategy = Closure.DELEGATE_FIRST
				// Call without parameters to allow property assignments
				clonedClosure.call()
			}
			is Map<*, *> -> {
				// Manually apply map properties to the bundle part
				extraConfig.forEach { (key, value) ->
					when (key.toString()) {
						"dependency" -> {
							if (value is Dependency) {
								bundlePart.dependency = value
							} else {
								throw GradleException("'dependency' must be of type Dependency, but was ${value?.javaClass?.name}")
							}
						}
						"name" -> bundlePart.name = value.toString()
						"jvmserver" -> bundlePart.jvmserver = value.toString()
						"versionRange" -> {
							if (bundlePart is OsgiBundlePartBinding) {
								bundlePart.versionRange = value.toString()
							}
						}
						else -> throw GradleException("Unknown property '$key' for bundle part configuration")
					}
				}
			}
			else -> {
				throw GradleException("'$extraConfig' cannot be used to configure a bundle part. Only Closure or Map are supported.")
			}
		}
		bundlePartsWithExtraConfig.add(bundlePart)
	}
}