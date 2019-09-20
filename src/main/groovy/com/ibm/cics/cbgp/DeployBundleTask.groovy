/*-
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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class DeployBundleTask extends AbstractBundleTask {

	public static final String MISSING_CONFIG = 'Missing or empty deploy configuration'
	public static final String MISSING_CICSPLEX = 'Specify cicsplex for deploy'
	public static final String MISSING_REGION = 'Specify region for deploy'
	public static final String MISSING_BUNDDEF = 'Specify bundle definition name for deploy'
	public static final String MISSING_CSDGROUP = 'Specify csd group for deploy'
	public static final String MISSING_URL = 'Specify url for deploy'
	public static final String MISSING_USERNAME = 'Specify username for deploy'
	public static final String MISSING_PASSWORD = 'Specify password for deploy'
	public static final String PLEASE_SPECIFY = 'Please specify deploy configuration'

	public static final String DEPLOY_CONFIG_EXCEPTION = PLEASE_SPECIFY + """\

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
"""

	@Input
	def deployExtension = project.extensions.getByName(BundlePlugin.DEPLOY_EXTENSION_NAME)

	@TaskAction
	def deployCICSBundle() {
		println "Task deployCICSBundle"

		validateDeployExtension()
	}

	private void validateDeployExtension() {
		def blockValid = true

		if (deployExtension.cicsplex.length() +
				deployExtension.region.length() +
				deployExtension.bunddef.length() +
				deployExtension.csdgroup.length() == 0) {
			logger.error(MISSING_CONFIG)
			blockValid = false
		} else {
			// Validate block items exist, no check on content
			if (deployExtension.cicsplex.length() == 0) {
				logger.error MISSING_CICSPLEX
				blockValid = false
			}
			if (deployExtension.region.length() == 0) {
				logger.error MISSING_REGION
				blockValid = false
			}
			if (deployExtension.bunddef.length() == 0) {
				logger.error MISSING_BUNDDEF
				blockValid = false
			}
			if (deployExtension.csdgroup.length() == 0) {
				logger.error MISSING_CSDGROUP
				blockValid = false
			}
			if (deployExtension.url.length() == 0) {
				logger.error MISSING_URL
				blockValid = false
			}
			if (deployExtension.username.length() == 0) {
				logger.error MISSING_USERNAME
				blockValid = false
			}
			if (deployExtension.password.length() == 0) {
				logger.error MISSING_PASSWORD
				blockValid = false
			}
		}

		// Throw exception if anything is wrong in the extension block
		if (!blockValid) {
			throw new GradleException(DEPLOY_CONFIG_EXCEPTION)
		}
	}
}