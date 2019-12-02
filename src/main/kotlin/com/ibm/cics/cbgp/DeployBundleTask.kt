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
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.net.URI

open class DeployBundleTask : AbstractBundleTask() {

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
				 ${BundlePlugin.DEPLOY_EXTENSION_NAME} {
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

	@Input
	val deployExtension = project.extensions.getByName(BundlePlugin.DEPLOY_EXTENSION_NAME) as DeployExtension

	@InputFile
	var inputFile: RegularFileProperty = project.objects.fileProperty()

	@TaskAction
	fun deployCICSBundle() {
		println("Task deployCICSBundle")

		validateDeployExtension()

		val bundle = inputFile.get().asFile
		val cicsplex = deployExtension.cicsplex
		val region = deployExtension.region
		val bunddef = deployExtension.bunddef
		val csdgroup = deployExtension.csdgroup
		val endpointURL = URI(deployExtension.url)
		val username = deployExtension.username
		val password = deployExtension.password
		val insecure = deployExtension.insecure

		BundleDeployHelper.deployBundle(endpointURL, bundle, bunddef, csdgroup, cicsplex, region, username, password, insecure)
	}

	private fun validateDeployExtension() {
		var blockValid = true

		if (deployExtension.cicsplex.length +
				deployExtension.region.length +
				deployExtension.bunddef.length +
				deployExtension.csdgroup.length == 0) {
			logger.error(MISSING_CONFIG)
			blockValid = false
		} else {
			// Validate block items exist, no check on content
			if (deployExtension.cicsplex.isEmpty()) {
				logger.error(MISSING_CICSPLEX)
				blockValid = false
			}
			if (deployExtension.region.isEmpty()) {
				logger.error(MISSING_REGION)
				blockValid = false
			}
			if (deployExtension.bunddef.isEmpty()) {
				logger.error(MISSING_BUNDDEF)
				blockValid = false
			}
			if (deployExtension.csdgroup.isEmpty()) {
				logger.error(MISSING_CSDGROUP)
				blockValid = false
			}
			if (deployExtension.url.isEmpty()) {
				logger.error(MISSING_URL)
				blockValid = false
			}
			if (deployExtension.username.isEmpty()) {
				logger.error(MISSING_USERNAME)
				blockValid = false
			}
			if (deployExtension.password.isEmpty()) {
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