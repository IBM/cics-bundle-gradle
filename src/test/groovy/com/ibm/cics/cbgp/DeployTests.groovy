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

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class DeployTests extends AbstractTest {

	// TODO Add deploy success tests once deploy task is performing deploy action.

	/**
	 * Common build.gradle contents for all tests.
	 */
	private String commonBuildFileContents

	def setup() {
		commonSetup(BundlePlugin.DEPLOY_TASK_NAME)
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		commonBuildFileContents = """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }

            version '1.0.0-SNAPSHOT'

            repositories {
                jcenter()
            }
            
            ${BundlePlugin.BUILD_EXTENSION_NAME} {
                defaultjvmserver = 'EYUCMCIJ'
            }
        """
	}

	def "Test missing deploy extension block"() {
		given:
		buildFile << """\
			${commonBuildFileContents}
        """

		when:
		def result = runGradleAndFail()

		then:
		checkResults(result, [DeployBundleTask.MISSING_CONFIG, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
	}

	def "Test empty deploy extension block"() {
		given:
		buildFile << """\
            ${commonBuildFileContents}

            ${BundlePlugin.DEPLOY_EXTENSION_NAME} {
            }

        """

		when:
		def result = runGradleAndFail()

		then:
		checkResults(result, [DeployBundleTask.MISSING_CONFIG, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
	}

	def "Test missing cicsplex"() {
		given:
		buildFile << """\
            ${commonBuildFileContents}

            ${BundlePlugin.DEPLOY_EXTENSION_NAME} {
                region = 'MYEGION'
                bunddef = 'MYDEF'
                csdgroup = 'MYGROUP'
                url = 'someurl'
                username = 'bob'
                password = 'passw0rd'
            }
        """

		when:
		def result = runGradleAndFail()

		then:
		checkResults(result, [DeployBundleTask.MISSING_CICSPLEX, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
	}

	def "Test missing region"() {
		given:
		buildFile << """\
            ${commonBuildFileContents}

            ${BundlePlugin.DEPLOY_EXTENSION_NAME} {
                cicsplex            = 'MYPLEX'
                bunddef             = 'MYDEF'
                csdgroup            = 'MYGROUP'
                url                 = 'someurl'
                username            = 'bob'
                password            = 'passw0rd'
            }
        """

		when:
		def result = runGradleAndFail()

		then:
		checkResults(result, [DeployBundleTask.MISSING_REGION, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
	}

	def "Test missing bunddef"() {
		given:
		buildFile << """\
            ${commonBuildFileContents}

            ${BundlePlugin.DEPLOY_EXTENSION_NAME} {
                cicsplex            = 'MYPLEX'
                region              = 'MYEGION'
                csdgroup            = 'MYGROUP'
                url                 = 'someurl'
                username            = 'bob'
                password            = 'passw0rd'
            }
        """

		when:
		def result = runGradleAndFail()

		then:
		checkResults(result, [DeployBundleTask.MISSING_BUNDDEF, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
	}

	def "Test missing csdgroup"() {
		given:
		buildFile << """\
            ${commonBuildFileContents}

            ${BundlePlugin.DEPLOY_EXTENSION_NAME} {
                cicsplex            = 'MYPLEX'
                region              = 'MYEGION'
                bunddef             = 'MYDEF'
                url                 = 'someurl'
                username            = 'bob'
                password            = 'passw0rd'
            }
        """

		when:
		def result = runGradleAndFail()

		then:
		checkResults(result, [DeployBundleTask.MISSING_CSDGROUP, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
	}

	def "Test missing url"() {
		given:
		buildFile << """\
            ${commonBuildFileContents}

            ${BundlePlugin.DEPLOY_EXTENSION_NAME} {
                cicsplex            = 'MYPLEX'
                region              = 'MYEGION'
                bunddef             = 'MYDEF'
                csdgroup            = 'MYGROUP'
                username = 'bob'
                password = 'passw0rd'
            }
        """

		when:
		def result = runGradleAndFail()

		then:
		checkResults(result, [DeployBundleTask.MISSING_URL, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
	}

	def "Test missing username"() {
		given:
		buildFile << """\
            ${commonBuildFileContents}

            ${BundlePlugin.DEPLOY_EXTENSION_NAME} {
                cicsplex            = 'MYPLEX'
                region              = 'MYEGION'
                bunddef             = 'MYDEF'
                csdgroup            = 'MYGROUP'
                url                 = 'someurl'
                password            = 'passw0rd'
            }
        """

		when:
		def result = runGradleAndFail()

		then:
		checkResults(result, [DeployBundleTask.MISSING_USERNAME, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
	}

	def "Test missing password"() {
		given:
		buildFile << """\
            ${commonBuildFileContents}

            ${BundlePlugin.DEPLOY_EXTENSION_NAME} {
                cicsplex            = 'MYPLEX'
                region              = 'MYEGION'
                bunddef             = 'MYDEF'
                csdgroup            = 'MYGROUP'
                url                 = 'someurl'
                username            = 'bob'
            }
        """

		when:
		def result = runGradleAndFail()

		then:
		checkResults(result, [DeployBundleTask.MISSING_PASSWORD, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
	}

	def "Test multiple items missing"() {
		given:
		buildFile << """\
            ${commonBuildFileContents}

            ${BundlePlugin.DEPLOY_EXTENSION_NAME} {
                cicsplex            = 'MYPLEX'
                csdgroup            = 'MYGROUP'
                url                 = 'someurl'
                username            = 'bob'
                password            = 'passw0rd'
            }
        """

		when:
		def result = runGradleAndFail()

		then:
		checkResults(result, [
				DeployBundleTask.MISSING_REGION,
				DeployBundleTask.MISSING_BUNDDEF,
				DeployBundleTask.PLEASE_SPECIFY
		], [], FAILED)
	}

	def "Test substitute username and password"() {
		given:
		File propertiesFile = testProjectDir.newFile('gradle.properties')
		propertiesFile << """\
            my_username=alice
            password=secret
        """

		buildFile << """\
            ${commonBuildFileContents}

            ${BundlePlugin.DEPLOY_EXTENSION_NAME} {
                region   = 'MYEGION'
                cicsplex = 'MYPLEX'
                bunddef  = 'MYDEF'
                csdgroup = 'MYGROUP'
                url      = 'someurl'
                username = my_username      // Define my_username in gradle.properties
                password = project.properties['password']  // same name for variable and gradle.properties entry
            }
        """

		when:
		def result = runGradleAndFail()

		then:
		// This error indicates success as it won't get this far if the substitution fails.
		checkResults(result, ["Target host is null"], [], FAILED)
	}
}
