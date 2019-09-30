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

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class BuildTests extends Specification {

	@Rule
	public TemporaryFolder testProjectDir = new TemporaryFolder()
	File settingsFile
	File buildFile

	boolean isDebug = ManagementFactory.getRuntimeMXBean().
			getInputArguments().toString().indexOf("-agentlib:jdwp") > 0

	def setup() {
		ExpandoMetaClass.disableGlobally()
		settingsFile = testProjectDir.newFile('settings.gradle')
		buildFile = testProjectDir.newFile('build.gradle')
	}

	def "Test jcenter jar dependency"() {
		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
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

            
            dependencies {
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}('javax.servlet:javax.servlet-api:3.1.0@jar')
            }
        """

		when:
		def result = runGradle()

		then:
		checkResults(result,
				['javax.servlet-api-3.1.0.jar', 'Task buildCICSBundle (Gradle 5.0)'],
				['cics-bundle-gradle-1.0.0-SNAPSHOT/javax.servlet-api-3.1.0.jar']
				, SUCCESS
		)
		printTemporaryFileTree()
	}

	def "Test maven war dependency"() {
		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                mavenCentral()
            }

            ${BundlePlugin.BUILD_EXTENSION_NAME} {
                defaultjvmserver = 'EYUCMCIJ'
            }
            
            dependencies {
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}(group: 'org.glassfish.main.admingui', name: 'war', version: '5.1.0', ext: 'war'  )
            }
        """

		when:
		def result = runGradle()

		then:
		checkResults(result, ['org.glassfish.main.admingui', 'war-5.1.0.war'], ['cics-bundle-gradle-1.0.0-SNAPSHOT/war-5.1.0.war'], SUCCESS)
	}

	def "Test maven ear dependency"() {
		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                mavenCentral()
            }
 
            ${BundlePlugin.BUILD_EXTENSION_NAME} {
                defaultjvmserver = 'EYUCMCIJ'
            }
           
            dependencies {
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}(group: 'org.codehaus.cargo', name: 'simple-ear', version: '1.7.6', ext: 'ear'  )
            }
        """

		when:
		def result = runGradle()

		then:
		checkResults(result, ['org.codehaus.cargo', 'simple-ear-1.7.6.ear'], ['cics-bundle-gradle-1.0.0-SNAPSHOT/simple-ear-1.7.6.ear'], SUCCESS)
	}

	def "Test local project dependency"() {

		def warProjectName = 'helloworldwar'

		File localBuildCacheDirectory
		localBuildCacheDirectory = testProjectDir.newFolder('local-cache')

		given:
		settingsFile << """\
            rootProject.name = 'cics-bundle-gradle'
            include '$warProjectName'
            
            buildCache {
                local {
                    directory '${localBuildCacheDirectory.toURI().toString()}'
                }
            }

            """
		buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                mavenCentral()
            }
            

            ${BundlePlugin.BUILD_EXTENSION_NAME} {
                defaultjvmserver = 'EYUCMCIJ'
            }
            dependencies {
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME} project(path: ':$warProjectName', configuration: 'war')
            }
        """

		// Copy the helloworldwar project into the test build
		def pluginClasspathResource = getClass().classLoader.findResource(warProjectName)
		if (pluginClasspathResource == null) {
			throw new IllegalStateException("Did not find $warProjectName resource.")
		}

		def root = new File(pluginClasspathResource.path).parent
		new AntBuilder().copy(todir: (buildFile.parent + "/" + warProjectName).toString()) {
			fileset(dir: (root + "/" + warProjectName).toString())
		}

		when:
		def result = runGradle(['build', BundlePlugin.BUILD_TASK_NAME])

		then:
		def builtWarName = "${warProjectName}-1.0-SNAPSHOT.war"
		def builtWarLocation = "cics-bundle-gradle-1.0.0-SNAPSHOT/$builtWarName"
		checkResults(result, ['Task :helloworldwar:build', builtWarName], [builtWarLocation], SUCCESS)
	}

	def "Test incorrect dependency extension"() {

		File localBuildCacheDirectory
		localBuildCacheDirectory = testProjectDir.newFolder('local-cache')

		given:
		settingsFile << """\
            rootProject.name = 'cics-bundle-gradle'
            
            buildCache {
                local {
                    directory '${localBuildCacheDirectory.toURI().toString()}'
                }
            }
            """

		buildFile << """\
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
            
            dependencies {
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}(group: 'org.apache.jmeter', name: 'apache-jmeter', version: '2.3.4-atlassian-1'  )
            }
        """

		when:
		def result = runGradle([BundlePlugin.BUILD_TASK_NAME], true)

		then:
		checkResults(result, [BuildBundleTask.UNSUPPORTED_EXTENSIONS_FOUND,
		                      "Unsupported file extension 'gz' for dependency 'apache-jmeter-2.3.4-atlassian-1.tar.gz'"]
				, [], FAILED)
	}

	def "Test no cicsBundle dependencies warning"() {

		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"

		buildFile << """\
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
           
            dependencies {
            }
        """

		when:
		def result = runGradle([BundlePlugin.BUILD_TASK_NAME], false)

		then:
		checkResults(result, [BuildBundleTask.NO_DEPENDENCIES_WARNING], [], SUCCESS)
	}

	def "Test missing defaultjvmserver in block"() {
		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                jcenter()
            }
 
            ${BundlePlugin.BUILD_EXTENSION_NAME} {
            }
           
            dependencies {
            }

        """

		when:
		def result = runGradle([BundlePlugin.BUILD_TASK_NAME], true)

		then:
		checkResults(result, [BuildBundleTask.MISSING_JVMSERVER, BuildBundleTask.PLEASE_SPECIFY], [], FAILED)
	}

	def "Test missing config block"() {
		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                jcenter()
            }
           
            dependencies {
            }

        """

		when:
		def result = runGradle([BundlePlugin.BUILD_TASK_NAME], true)

		then:
		checkResults(result, [BuildBundleTask.MISSING_JVMSERVER, BuildBundleTask.PLEASE_SPECIFY], [], FAILED)
	}

	def "Test build -> packageCICSBundle -> buildCICSbundle task dependency"() {
		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }

            version '1.0.0-SNAPSHOT'

            ${BundlePlugin.BUILD_EXTENSION_NAME} {
                defaultjvmserver = 'EYUCMCIJ'
            }

        """

		when:
		def result = runGradle(['build'], false)

		then:
		checkResults(result, ['> Task :buildCICSBundle', '> Task :packageCICSBundle NO-SOURCE'], [], SUCCESS)
	}

    // TODO Restore when zipfile is built
//	def "Test packageCICSBundle produces zip in default location"() {
//		given:
//		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
//		buildFile << """\
//            plugins {
//                id 'cics-bundle-gradle-plugin'
//            }
//
//            version '1.0.0-SNAPSHOT'
//
//            repositories {
//                mavenCentral()
//            }
//
//            ${BundlePlugin.BUILD_EXTENSION_NAME} {
//                defaultjvmserver = 'EYUCMCIJ'
//            }
//
//            dependencies {
//                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME} 'org.codehaus.cargo:simple-ear:1.7.6@ear'
//            }
//
//        """
//
//		when:
//		def result = runGradle(['packageCICSBundle'], false)
//
//		then:
//		checkResults(result, ['> Task :buildCICSBundle\n', '> Task :packageCICSBundle\n'], ['distributions/cics-bundle-gradle-1.0.0-SNAPSHOT.zip'], SUCCESS)
//	}

	// Run the gradle build with defaults and print the test output
	def runGradle(List args = [BundlePlugin.BUILD_TASK_NAME], boolean failExpected = false) {
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
		def title = "\n----- '$specificationContext.currentIteration.name' output: -----"
		println(title)
		println(result.output)
		println('-' * title.length())
		println()
		return result
	}

	// TODO Add tests for cics.xml contents
	def checkResults(BuildResult result, List resultStrings, List outputFiles, TaskOutcome outcome) {
		resultStrings.each {
			assert result.output.contains(it)
		}
		outputFiles.each {
            // TODO - Resume checks when bundle publisher is working
//			assert getFileInBuildOutputFolder(it).exists()
		}
		result.task(":$BundlePlugin.BUILD_TASK_NAME").outcome == outcome
	}

	private File getFileInBuildOutputFolder(String fileName) {
		return new File(buildFile.parent + '/build/' + fileName)
	}

	// Some useful functions as I can't get debug to work for Gradle Runner tests yet

	/*
	  Print out the file tree after the test excluding hidden files.
	  Print out cics.xml if found
	 */
	private void printTemporaryFileTree() {
		def tempFolder = new File(buildFile.parent)
		def cicsxmlName = ""

		def title = "\n----- '$specificationContext.currentIteration.name' Output file tree: $tempFolder -----"
		println(title)
		int prefixSize = tempFolder.toString().size() + 1
		tempFolder.traverse {
			def pathString = it.path.toString()
			if (!pathString.contains("/.")) {
				println('   ' + pathString.substring(prefixSize))
			}
			if (pathString.endsWith("cics.xml")) {
				cicsxmlName = pathString
			}
		}
		if (!cicsxmlName.isEmpty()) {
			println("\ncics.xml contains\n-----------------")
			def lnum = 1
			new File(cicsxmlName).eachLine {
				line ->
					def prtnum = String.format( "%03d", lnum )
					println "   $prtnum: $line"
					lnum++
			}
			println("end of cics.xml\n")
		}
		println('-' * title.length())
		println()
	}

}
