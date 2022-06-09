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

import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import spock.lang.Specification

import java.lang.management.ManagementFactory

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

abstract class AbstractTest extends Specification {

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort())

	protected boolean isDebug = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0

	protected String rootProjectName
	protected File rootProjectDir
	protected String bundleProjectName
	protected File bundleProjectDir
	protected String projectVersion = "1.0.0"
	protected String gradlePropertiesFilename = "gradle.properties"

	/**
	 * Set to false and set username and password in gradle.properties
	 * if you want to deploy to CICSEX56 for real.
	 */
	protected boolean wireMock = true
	protected String url

	def setup() {
		ExpandoMetaClass.disableGlobally()
		if (wireMock) {
			WireMock.setupWiremock()
			url = "http://localhost:${wireMockRule.port()}"
		} else {
			url = "https://cicsex56.hursley.ibm.com:28951"
		}
	}

	def getTestName() {
		return "$specificationContext.currentIteration.name"
	}

	protected def getBundleNameAndVersion() {
		return "${bundleProjectName}-${projectVersion}"
	}

	protected static def getFileInDir(File directory, String fileName) {
		return new File(directory.path + "/$fileName")
	}

	protected static def getTestResourcesDir() {
		return new File("build/resources/test")
	}

	protected static def getTestProjectsDir() {
		return new File("build/test-projects")
	}

	protected def getBuildFile() {
		return getFileInDir(bundleProjectDir, "build.gradle")
	}

	protected def getGradlePropertiesFile() {
		return getFileInDir(bundleProjectDir, "gradle.properties")
	}

	protected def getGradleProperties() {
		Properties properties = new Properties()
		gradlePropertiesFile.withInputStream {
			properties.load(it)
		}
		return properties
	}

	protected def getBuildDir() {
		return getFileInDir(bundleProjectDir, "build")
	}

	protected def getDistributionsDir() {
		return getFileInDir(buildDir, "distributions")
	}

	protected def getArchiveFile() {
		return getFileInDir(distributionsDir, "${bundleNameAndVersion}.zip")
	}

	protected def getBundleBuildDir() {
		return getFileInDir(buildDir, "${bundleNameAndVersion}")
	}

	protected def getManifestFile() {
		return getFileInDir(bundleBuildDir, "META-INF/cics.xml")
	}

	protected def copyTestProject() {

		File srcFile = getFileInDir(testResourcesDir, rootProjectName)
		File destFile = getFileInDir(testProjectsDir, "$testName/$rootProjectName")
		FileUtils.copyDirectory(srcFile, destFile)
		rootProjectDir = destFile
		if (bundleProjectName == rootProjectName) {
			bundleProjectDir = rootProjectDir
		} else {
			bundleProjectDir = getFileInDir(rootProjectDir, bundleProjectName)
		}
		copyGradleProperties()
	}

	protected def copyGradleProperties() {
		File srcFile = getFileInDir(testResourcesDir, gradlePropertiesFilename)
		File destFile = gradlePropertiesFile
		FileUtils.copyFile(srcFile, destFile)
	}

	protected def runGradleAndSucceed(List args) {
		return runGradle(args, false)
	}

	protected def runGradleAndFail(List args) {
		return runGradle(args, true)
	}

	// Run the gradle build and print the test output
	private def runGradle(List args, boolean failExpected) {
		def result
		args.add("--stacktrace")
		args.add("--info")
		args.add("-Purl=$url".toString())
		GradleRunner gradleRunner = GradleRunner
				.create()
				.withProjectDir(rootProjectDir)
				.withArguments(args)
				.withPluginClasspath()
				.withDebug(isDebug)
				.withGradleVersion("7.4.2")

		if (!failExpected) {
			result = gradleRunner.build()
		} else {
			result = gradleRunner.buildAndFail()
		}
		printOutput(result)
		return result
	}

	/**
	 * Print the Gradle build output, the list of files in the test project excluding hidden files, and the contents
	 * of cics.xml if it exists.
	 * @param result
	 */
	private void printOutput(BuildResult result) {

		printGradleOutput(result)
		printFileTree()
		printManifestContents()
	}

	private void printGradleOutput(BuildResult result) {

		println("----- '$testName' Gradle output: -----")
		println()
		println(result.output)
		println("----- End Gradle output -----")
		println()
	}

	private void printFileTree() {

		println("----- '$testName' File tree: $rootProjectDir -----")
		println()
		rootProjectDir.traverse { file ->
			def path = rootProjectDir.relativePath(file)
			if (!path.contains("/.")) {
				println('   ' + path)
			}
		}
		println()
		println("----- End File tree -----")
		println()
	}

	private void printManifestContents() {

		def manifestFile = getManifestFile()
		if (manifestFile.exists()) {
			println("----- '$testName' cics.xml: -----")
			println()
			def lineNumber = 1
			manifestFile.eachLine { line ->
				def lineNumber3Digits = String.format("%03d", lineNumber)
				println "   $lineNumber3Digits: $line"
				lineNumber++
			}
			println()
			println("----- End cics.xml -----")
			println()
		}
	}

	protected static checkBuildOutputStrings(BuildResult result, List<String> expectedOutputStrings) {
		expectedOutputStrings.each { expectedOutputString ->
			assert result.output.contains(expectedOutputString): "Missing output string '$expectedOutputString'"
		}
	}

	protected checkBuildOutputFiles(List<String> expectedOutputFiles) {
		expectedOutputFiles.each { expectedOutputFile ->
			assert getFileInDir(getBundleBuildDir(), expectedOutputFile).exists(): "Missing output file '${expectedOutputFile}'"
		}
	}

	protected static checkFileContains(File file, List<String> expectedStrings) {
		assert file.exists(): "Unable to find ${file.name}"
		def lines = file.readLines()
		expectedStrings.each { expectedString ->
			def found = lines.find { line ->
				return line.contains(expectedString)
			}
			if (!found) {
				assert false: "${file.name} is missing : '$expectedString'"
			}
		}
	}

	protected checkManifest(List<String> expectedStrings) {
		checkFileContains(manifestFile, expectedStrings)
	}

	protected void checkBundleArchiveFile() {
		assert archiveFile.exists(): "Missing archive file '${archiveFile.getPath()}'"
	}
}