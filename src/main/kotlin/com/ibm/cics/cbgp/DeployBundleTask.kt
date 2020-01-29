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

import com.ibm.cics.bundle.deploy.BundleDeployHelper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.net.URI

open class DeployBundleTask : DefaultTask() {

	companion object {
		const val MISSING_CONFIG = "Missing or empty deploy configuration"
		const val MISSING_CICSPLEX = "Specify cicsplex for deploy"
		const val MISSING_REGION = "Specify region for deploy"
		const val MISSING_BUNDDEF = "Specify bundle definition name for deploy"
		const val MISSING_CSDGROUP = "Specify csd group for deploy"
		const val MISSING_URL = "Specify url for deploy"
		const val MISSING_USERNAME = "Specify username for deploy"
		const val MISSING_PASSWORD = "Specify password for deploy"
		const val PLEASE_SPECIFY = "Please specify deploy configuration"

		val DEPLOY_CONFIG_EXCEPTION =
			PLEASE_SPECIFY + """\

			Example:
				 ${BundlePlugin.BUNDLE_EXTENSION_NAME} {
					cicsplex = 'MYPLEX'
					region   = 'MYEGION'
					bunddef  = 'MYDEF'
					csdgroup = 'MYGROUP'
					url      = 'myserver.site.domain.com'
					username = my_username      // Define my_username in gradle.properties
					password = my_password      // Define my_password in gradle.properties      
				} 
				  
				All items must be completed.
			""".trimIndent()
	}

	/**
	 * Set parameters from the cicsBundle extension as task inputs.
	 */
	@Internal
	val bundleExtension = project.extensions.getByName(BundlePlugin.BUNDLE_EXTENSION_NAME) as BundleExtension
	@Input
	var cicsplex = bundleExtension.cicsplex
	@Input
	var region = bundleExtension.region
	@Input
	var bunddef = bundleExtension.bunddef
	@Input
	var csdgroup = bundleExtension.csdgroup
	@Input
	var url = bundleExtension.url
	@Input
	var username = bundleExtension.username
	@Input
	var password = bundleExtension.password
	@Input
	var insecure = bundleExtension.insecure

	/**
	 * Set the bundle archive file as a task input. This will be linked to the output of the package task.
	 */
	@InputFile
	var inputFile: RegularFileProperty = project.objects.fileProperty()

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

		if (cicsplex.length +
				region.length +
				bunddef.length +
				csdgroup.length == 0) {
			logger.error(MISSING_CONFIG)
			blockValid = false
		} else {
			// Validate block items exist, no check on content
			if (cicsplex.isEmpty()) {
				logger.error(MISSING_CICSPLEX)
				blockValid = false
			}
			if (region.isEmpty()) {
				logger.error(MISSING_REGION)
				blockValid = false
			}
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
		}

		// Throw exception if anything is wrong in the extension block
		if (!blockValid) {
			throw GradleException(DEPLOY_CONFIG_EXCEPTION)
		}
	}
}