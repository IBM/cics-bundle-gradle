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

import org.apache.commons.io.FileUtils
import spock.lang.Unroll

import java.nio.charset.Charset

/**
 * Test error path scenarios where invalid bundles fail to build and return appropriate error messages.
 */
class ErrorTests extends AbstractTest {

	def "Test unsupported bundle part extension"() {

		given:
		rootProjectName = bundleProjectName = "empty"

		copyTestProject()

		buildFile << """
		dependencies {
			cicsBundlePart(group: 'org.codehaus.cargo', name: 'simple-har', version: '1.7.7', ext: 'har')
		}
		""".stripIndent()

		when:
		def result = runGradleAndFail([BundlePlugin.DEPLOY_TASK_NAME])

		then:
		checkBuildOutputStrings(result, ["Unsupported file extension 'har' for Java-based bundle part 'simple-har-1.7.7.har'. Supported extensions are: [ear, jar, war, eba]."])
	}

	@Unroll
	def "Test cicsBundle extension missing #propertiesToRemove"(List<String> propertiesToRemove, List<String> expectedMessages) {

		given:
		rootProjectName = bundleProjectName = "standalone-war"

		copyTestProject()

		when:
		def fileLines = FileUtils.readLines(buildFile, Charset.defaultCharset())
		propertiesToRemove.forEach { propertyToRemove ->
			fileLines.removeAll { fileLine ->
				fileLine.replaceAll("\\s", "").startsWith("$propertyToRemove=")
			}
		}
		FileUtils.writeLines(buildFile, fileLines)
		def result = runGradleAndFail([BundlePlugin.DEPLOY_TASK_NAME])

		then:
		checkBuildOutputStrings(result, expectedMessages)

		// Parameterize test so the same test can be used for various combinations of properties
		where:
		propertiesToRemove | expectedMessages
		["defaultJVMServer"] | [AbstractJavaBundlePartBinding.JVMSERVER_EXCEPTION]
		["url"]              | [DeployBundleTask.MISSING_URL, DeployBundleTask.DEPLOY_CONFIG_EXCEPTION]
		["bunddef"]          | [DeployBundleTask.MISSING_BUNDDEF, DeployBundleTask.DEPLOY_CONFIG_EXCEPTION]
		["csdgroup"]         | [DeployBundleTask.MISSING_CSDGROUP, DeployBundleTask.DEPLOY_CONFIG_EXCEPTION]
		["username"]         | [DeployBundleTask.MISSING_USERNAME, DeployBundleTask.DEPLOY_CONFIG_EXCEPTION]
		["password"]         | [DeployBundleTask.MISSING_PASSWORD, DeployBundleTask.DEPLOY_CONFIG_EXCEPTION]
		["url", "bunddef", "csdgroup", "username", "password"] | [DeployBundleTask.MISSING_URL, DeployBundleTask.MISSING_BUNDDEF, DeployBundleTask.MISSING_CSDGROUP, DeployBundleTask.MISSING_USERNAME, DeployBundleTask.MISSING_PASSWORD, DeployBundleTask.DEPLOY_CONFIG_EXCEPTION]
	}
}