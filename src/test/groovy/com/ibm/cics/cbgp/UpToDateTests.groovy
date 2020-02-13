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

import java.nio.charset.Charset

class UpToDateTests extends AbstractTest {

	def "Test build up-to-date"() {

		given:
		rootProjectName = bundleProjectName = "empty"

		copyTestProject()

		println("----- '$testName' - Run build -----")
		when:
		def result = runGradleAndSucceed([BundlePlugin.BUILD_TASK_NAME])

		then:
		checkBuildOutputStrings(result, [
				"Task ':buildCICSBundle' is not up-to-date"
		])

		println("----- '$testName' - Run build again with no changes -----")
		when:
		result = runGradleAndSucceed([BundlePlugin.BUILD_TASK_NAME])

		then:
		checkBuildOutputStrings(result, [
				"Skipping task ':buildCICSBundle' as it is up-to-date"
		])

		println("----- '$testName' - Add Java-based bundle part -----")
		when:
		buildFile << """
		dependencies {
			cicsBundle(group: 'org.codehaus.cargo', name: 'simple-bundle', version: '1.7.7', ext: 'jar')
		}
		""".stripIndent()
		result = runGradleAndSucceed([BundlePlugin.BUILD_TASK_NAME])

		then:
		checkBuildOutputStrings(result, [
				"Task ':buildCICSBundle' is not up-to-date"
		])

		println("----- '$testName' - Add non-Java-based bundle part -----")
		when:
		File srcFile = getFileInDir(bundleProjectDir, "resources")
		File destFile = resourcesDir
		FileUtils.moveDirectory(srcFile, destFile)
		result = runGradleAndSucceed([BundlePlugin.BUILD_TASK_NAME])

		then:
		checkBuildOutputStrings(result, [
				"Task ':buildCICSBundle' is not up-to-date"
		])

		println("----- '$testName' - Delete build output dir -----")
		when:
		bundleBuildDir.deleteDir()
		result = runGradleAndSucceed([BundlePlugin.BUILD_TASK_NAME])

		then:
		checkBuildOutputStrings(result, [
				"Task ':buildCICSBundle' is not up-to-date"
		])

		println("----- '$testName' - Change defaultJVMServer -----")
		when:
		def fileLines = FileUtils.readLines(buildFile, Charset.defaultCharset())
		def lineToReplace = fileLines.find() { fileLine ->
			fileLine.replaceAll("\\s", "").startsWith("defaultJVMServer=")
		}
		Collections.replaceAll(fileLines, lineToReplace, "defaultJVMServer = 'NEW'")
		FileUtils.writeLines(buildFile, fileLines)
		result = runGradleAndSucceed([BundlePlugin.BUILD_TASK_NAME])

		then:
		checkBuildOutputStrings(result, [
				"Task ':buildCICSBundle' is not up-to-date"
		])
	}

	def "Test package up-to-date"() {

		given:
		rootProjectName = bundleProjectName = "empty"

		copyTestProject()

		println("----- '$testName' - Run build -----")
		when:
		def result = runGradleAndSucceed([BundlePlugin.PACKAGE_TASK_NAME])

		then:
		checkBuildOutputStrings(result, [
				"Task ':packageCICSBundle' is not up-to-date"
		])

		println("----- '$testName' - Run build again with no changes -----")
		when:
		result = runGradleAndSucceed([BundlePlugin.PACKAGE_TASK_NAME])

		then:
		checkBuildOutputStrings(result, [
				"Skipping task ':packageCICSBundle' as it is up-to-date"
		])

		println("----- '$testName' - Delete build output dir -----")
		when:
		bundleBuildDir.deleteDir()
		result = runGradleAndSucceed([BundlePlugin.PACKAGE_TASK_NAME])

		then:
		checkBuildOutputStrings(result, [
				"Task ':packageCICSBundle' is not up-to-date"
		])

		println("----- '$testName' - Delete archive zip -----")
		when:
		archiveFile.delete()
		result = runGradleAndSucceed([BundlePlugin.PACKAGE_TASK_NAME])

		then:
		checkBuildOutputStrings(result, [
				"Task ':packageCICSBundle' is not up-to-date"
		])
	}

	def "Test deploy up-to-date"() {

		given:
		rootProjectName = bundleProjectName = "empty"

		copyTestProject()

		println("----- '$testName' - Run build -----")
		when:
		def result = runGradleAndSucceed([BundlePlugin.DEPLOY_TASK_NAME])

		then:
		checkBuildOutputStrings(result, [
				"Task ':deployCICSBundle' is not up-to-date"
		])

		println("----- '$testName' - Run build again with no changes -----")
		when:
		result = runGradleAndSucceed([BundlePlugin.DEPLOY_TASK_NAME])

		then:
		// Deploy task should always run, even if there are no changes
		checkBuildOutputStrings(result, [
				"Task ':deployCICSBundle' is not up-to-date"
		])
	}
}