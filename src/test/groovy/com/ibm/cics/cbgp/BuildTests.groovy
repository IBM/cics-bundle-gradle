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

class BuildTests extends AbstractTest {

	// TODO Add checks for cics.xml contents

	def setup() {
		commonSetup(BundlePlugin.BUILD_TASK_NAME)
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
				['cics-bundle-gradle-1.0.0-SNAPSHOT/javax.servlet-api_3.1.0.osgibundle']
				, SUCCESS
		)

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
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}(group: 'org.glassfish.main.admingui', name: 'war', version: '5.1.0', ext: 'war')
            }
        """

		when:
		def result = runGradle()

		then:
		checkResults(result,
				['org.glassfish.main.admingui', 'war-5.1.0.war'],
				['cics-bundle-gradle-1.0.0-SNAPSHOT/war.warbundle'],
				SUCCESS)
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
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}(group: 'org.codehaus.cargo', name: 'simple-ear', version: '1.7.6', ext: 'ear')
            }
        """

		when:
		def result = runGradle()

		then:
		checkResults(result,
				['org.codehaus.cargo', 'simple-ear-1.7.6.ear'],
				['cics-bundle-gradle-1.0.0-SNAPSHOT/simple-ear.earbundle']
				, SUCCESS)
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
		checkResults(result,
				['Task :helloworldwar:build', "${warProjectName}-1.0-SNAPSHOT.war"],
				["cics-bundle-gradle-1.0.0-SNAPSHOT/helloworldwar.warbundle"],
				SUCCESS)
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
		def result = runGradleAndFail()

		then:
		checkResults(result,
				[BuildBundleTask.UNSUPPORTED_EXTENSIONS_FOUND, "Unsupported file extension 'gz' for dependency 'apache-jmeter-2.3.4-atlassian-1.tar.gz'"],
				[],
				FAILED)
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
		def result = runGradle()

		then:
		checkResults(result,
				[BuildBundleTask.NO_DEPENDENCIES_WARNING],
				[],
				SUCCESS)
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
		def result = runGradleAndFail()

		then:
		checkResults(result,
				[BuildBundleTask.MISSING_JVMSERVER, BuildBundleTask.PLEASE_SPECIFY],
				[],
				FAILED)
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
		def result = runGradleAndFail()

		then:
		checkResults(result,
				[BuildBundleTask.MISSING_JVMSERVER, BuildBundleTask.PLEASE_SPECIFY],
				[],
				FAILED)
	}


	def "Test packageCICSBundle produces zip in default location"() {
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
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME} 'org.codehaus.cargo:simple-ear:1.7.6@ear'
            }

        """

		when:
		def result = runGradle(['packageCICSBundle'], false)

		then:
		checkResults(result,
				['> Task :buildCICSBundle\n', '> Task :packageCICSBundle\n'],
				['distributions/cics-bundle-gradle-1.0.0-SNAPSHOT.zip']
				, SUCCESS)
	}

}
