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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.lang.management.ManagementFactory

abstract class AbstractTest extends Specification {

	@Rule
	public TemporaryFolder testProjectDir = new TemporaryFolder()
	protected File settingsFile
	protected File buildFile
	protected boolean isDebug = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0

	private defaultTask

	def commonSetup(String defaultTask) {
		this.defaultTask = defaultTask
		ExpandoMetaClass.disableGlobally()
		settingsFile = testProjectDir.newFile('settings.gradle')
		buildFile = testProjectDir.newFile('build.gradle')
	}

	def cleanup() {
		def tempFolder = new File(buildFile.parent)
		assert tempFolder.deleteDir(): "Failed to clean-up test folder '$tempFolder'"
	}

	protected def runGradleAndFail() {
		return runGradle([defaultTask], true)
	}

	// Run the gradle build with defaults and print the test output
	protected def runGradle(List args = [defaultTask], boolean failExpected = false) {
		def result
		args.add("--stacktrace")
		args.add("--info")
		if (!failExpected) {
			result = GradleRunner
					.create()
					.withProjectDir(testProjectDir.root)
					.withArguments(args)
					.withPluginClasspath()
					.withDebug(isDebug)
					.build()
		} else {
			result = GradleRunner
					.create()
					.withProjectDir(testProjectDir.root)
					.withArguments(args)
					.withPluginClasspath()
					.withDebug(isDebug)
					.buildAndFail()
		}
		printRunOutput(result)
		return result
	}

	private void printRunOutput(BuildResult result) {
		def title = "\n----- '$specificationContext.currentIteration.name' output: -----"
		println(title)
		println(result.output)
		println('-' * title.length())
		println()
	}

	protected def checkResults(BuildResult result, List resultStrings, List outputFiles, TaskOutcome outcome) {
		printTemporaryFileTree()
		resultStrings.each {
			assert result.output.contains(it): "Missing output string '$it'"
		}
		outputFiles.each {
			assert getFileInBuildOutputFolder(it).exists(): "Missing output file '${it.toString()}'"
		}
		result.task(":$defaultTask").outcome == outcome
	}

	protected def checkManifest(List manifestStrings) {
		def manifestFile = getFileInBuildOutputFolder('cics-bundle-gradle-1.0.0-SNAPSHOT/META-INF/cics.xml')
		assert manifestFile.exists(): "Unable to find cics.xml"
		def lines = manifestFile.readLines()
		manifestStrings.each { searchString ->
			def found = lines.find { line ->
				if (line.contains(searchString)) {
					return true
				}
			}
			if (!found) assert false: "cics.xml is missing : '$searchString'"
		}
	}

	// Print out the file tree after the test excluding hidden files.
	// Print out cics.xml if found
	protected void printTemporaryFileTree() {
		def tempFolder = new File(buildFile.parent)
		def manifestFilename = ""

		def title = "\n----- '$specificationContext.currentIteration.name' Output file tree: $tempFolder -----"
		println(title)
		int prefixSize = tempFolder.toString().size() + 1
		tempFolder.traverse {
			def pathString = it.path.toString()
			if (!pathString.contains("/.")) {
				println('   ' + pathString.substring(prefixSize))
			}
			if (pathString.endsWith("cics.xml")) {
				manifestFilename = pathString
			}
		}

		if (!manifestFilename.isEmpty()) {
			println("\ncics.xml contains\n-----------------")
			def lnum = 1
			new File(manifestFilename).eachLine {
				line ->
					def prtnum = String.format("%03d", lnum)
					println "   $prtnum: $line"
					lnum++
			}
			println("end of cics.xml\n")
		}
		println('-' * title.length())
		println()
	}

	protected File getFileInBuildOutputFolder(String fileName) {
		return new File(buildFile.parent + '/build/' + fileName)
	}

	protected def copyBundlePartsToResources(String source_folder) {
		// Copy the bundle part into the test build
		def pluginClasspathResource = getClass().classLoader.findResource(source_folder)
		if (pluginClasspathResource == null) {
			throw new IllegalStateException("Did not find $partFileName resource.")
		}

		def root = new File(pluginClasspathResource.path).parent
		new AntBuilder().copy(todir: (buildFile.parent + "/src/main/resources/")) {
			fileset(dir: (root + "/" + source_folder).toString())
		}
	}
}
