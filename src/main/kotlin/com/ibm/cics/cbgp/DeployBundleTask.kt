/*
 * #%L
 * CICS Bundle Gradle Plugin
 * %%
 * Copyright (C) 2019,2022 IBM Corp.
 * %%
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * #L%
 */
package com.ibm.cics.cbgp

import com.ibm.cics.bundle.deploy.BundleDeployHelper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.net.URI

open class DeployBundleTask : DefaultTask() {

	companion object {
		const val MISSING_BUNDDEF = "Specify bundle definition name for deploy"
		const val MISSING_CSDGROUP = "Specify csd group for deploy"
		const val MISSING_URL = "Specify url for deploy"
		const val MISSING_USERNAME = "Specify username for deploy"
		const val MISSING_PASSWORD = "Specify password for deploy"
		const val MISSING_CICSPLEX_OR_REGION = "Specify both or neither of cicsplex and region for deploy"

		val DEPLOY_CONFIG_EXCEPTION = """
			Please specify deploy configuration in build.gradle.
			Example:
				${BundlePlugin.BUNDLE_EXTENSION_NAME} {
					deploy {
						cicsplex = 'MYPLEX'
						region   = 'MYREGION'
						bunddef  = 'MYDEF'
						csdgroup = 'MYGROUP'
						url      = 'https://hostname.com:port'
						username = my_username
						password = my_password 
					}
				}
			All items must be completed.
			""".trimIndent()
	}

	init {
		// Never skip the deploy task, even if nothing has changed, because we can't know whether the bundle needs redeploying in CICS.
		outputs.upToDateWhen { false }
	}

	/**
	 * Set parameters from the cicsBundle extension as task inputs.
	 */
	@Internal
	val bundleExtension = project.extensions.getByName(BundlePlugin.BUNDLE_EXTENSION_NAME) as BundleExtension
	@Input
	val cicsplex = bundleExtension.deploy.cicsplex
	@Input
	val region = bundleExtension.deploy.region
	@Input
	val bunddef = bundleExtension.deploy.bunddef
	@Input
	val csdgroup = bundleExtension.deploy.csdgroup
	@Input
	val url = bundleExtension.deploy.url
	@Input
	val username = bundleExtension.deploy.username
	@Input
	val password = bundleExtension.deploy.password.toCharArray()
	@Input
	val insecure = bundleExtension.deploy.insecure

	/**
	 * Set the bundle archive file as a task input. This will be linked to the output of the package task.
	 */
	@InputFile
	val inputFile: RegularFileProperty = project.objects.fileProperty()

	@TaskAction
	fun deployCICSBundle() {
		println("Task deployCICSBundle")

		validateBundleExtension()

		val bundle = inputFile.get().asFile
		val endpointURL = URI(url)
		BundleDeployHelper.deployBundle(endpointURL, bundle, bunddef, csdgroup, cicsplex, region, username, password, insecure)
	}

	private fun validateBundleExtension() {
		var blockValid = true
		var cicsplexSpecified = !cicsplex.isEmpty()
		var regionSpecified = !region.isEmpty()

		// Validate block items exist, no check on content
		if (bunddef.isEmpty()) {
			logger.error(MISSING_BUNDDEF)
			blockValid = false
		}
		if (csdgroup.isEmpty()) {
			logger.error(MISSING_CSDGROUP)
			blockValid = false
		}
		if (url.isEmpty()) {
			logger.error(MISSING_URL)
			blockValid = false
		}
		if (username.isEmpty()) {
			logger.error(MISSING_USERNAME)
			blockValid = false
		}
		if (password.isEmpty()) {
			logger.error(MISSING_PASSWORD)
			blockValid = false
		}
		if(regionSpecified && !cicsplexSpecified || !regionSpecified && cicsplexSpecified) {
			logger.error(MISSING_CICSPLEX_OR_REGION)
			blockValid = false
		}

		// Throw exception if anything is wrong in the extension block
		if (!blockValid) {
			throw GradleException(DEPLOY_CONFIG_EXCEPTION)
		}
	}
}