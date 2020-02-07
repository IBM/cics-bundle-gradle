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

	// TODO Test that default JVMServer value is used if not specified in extension.

	/**
	 * Common build.gradle contents for most tests.
	 */
	private String commonBuildFileContents
	private String pluginDetails

	def setup() {
		commonSetup(BundlePlugin.BUILD_TASK_NAME)
		pluginDetails = """\
            plugins {
                id 'com.ibm.cics.bundle'
            }

            version '1.0.0-SNAPSHOT'

            repositories {
                jcenter()
                mavenCentral()
            }
        """
		commonBuildFileContents = """\
            ${pluginDetails}

            ${BundlePlugin.BUNDLE_EXTENSION_NAME} {
                defaultJVMServer = 'MYJVMS'
            }
        """
	}

	def "Test build empty bundle"() {
		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
           ${pluginDetails}
        """

		when:
		def result = runGradle()

		then:
		checkResults(result,
				['Task buildCICSBundle (Gradle 5.0)'],
				[],
				SUCCESS
		)

		checkManifest(['id="cics-bundle-gradle">'])
		checkManifestDoesNotContain(['<define '])
	}

	def "Test jcenter jar dependency"() {
		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
           ${commonBuildFileContents}

            dependencies {
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}('javax.servlet:javax.servlet-api:3.1.0@jar')
            }
        """

		when:
		def result = runGradle()

		then:
		checkResults(result,
				['javax.servlet-api-3.1.0.jar'
				],
				['cics-bundle-gradle-1.0.0-SNAPSHOT/javax.servlet-api-3.1.0_3.1.0.osgibundle',
				 'cics-bundle-gradle-1.0.0-SNAPSHOT/javax.servlet-api-3.1.0_3.1.0.jar'
				],
				SUCCESS
		)

		checkManifest(['id="cics-bundle-gradle">',
		               '<define name="javax.servlet-api-3.1.0_3.1.0" path="javax.servlet-api-3.1.0_3.1.0.osgibundle" type="http://www.ibm.com/xmlns/prod/cics/bundle/OSGIBUNDLE"/>'
		])
	}

	def "Test maven war dependency"() {
		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
           ${commonBuildFileContents}
            
            dependencies {
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}(group: 'org.glassfish.main.admingui', name: 'war', version: '5.1.0', ext: 'war')
            }
        """

		when:
		def result = runGradle()

		then:
		checkResults(result,
				['org.glassfish.main.admingui',
				 'war-5.1.0.war'
				],
				['cics-bundle-gradle-1.0.0-SNAPSHOT/war-5.1.0.warbundle',
				 'cics-bundle-gradle-1.0.0-SNAPSHOT/war-5.1.0.war'
				],
				SUCCESS)

		checkManifest(['id="cics-bundle-gradle">',
		               '<define name="war-5.1.0" path="war-5.1.0.warbundle" type="http://www.ibm.com/xmlns/prod/cics/bundle/WARBUNDLE"/>'
		])
	}

	def "Test maven ear dependency"() {
		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
            ${commonBuildFileContents}
           
            dependencies {
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}(group: 'org.codehaus.cargo', name: 'simple-ear', version: '1.7.6', ext: 'ear')
            }
        """

		when:
		def result = runGradle()

		then:
		checkResults(result,
				['org.codehaus.cargo',
				 'simple-ear-1.7.6.ear'
				],
				['cics-bundle-gradle-1.0.0-SNAPSHOT/simple-ear-1.7.6.earbundle',
				 'cics-bundle-gradle-1.0.0-SNAPSHOT/simple-ear-1.7.6.ear'
				],
				SUCCESS)

		checkManifest(['id="cics-bundle-gradle">',
		               '<define name="simple-ear-1.7.6" path="simple-ear-1.7.6.earbundle" type="http://www.ibm.com/xmlns/prod/cics/bundle/EARBUNDLE"/>'
		])
	}

	def "Test maven EBA dependency"() {
		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
            ${commonBuildFileContents}
           
            dependencies {
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}(group: 'org.apache.aries.samples.twitter', name: 'org.apache.aries.samples.twitter.eba', version: '1.0.0', ext: 'eba')
            }
        """

		when:
		def result = runGradle()

		then:
		checkResults(result,
				['org.apache.aries.samples.twitter',
				 'org.apache.aries.samples.twitter.eba-1.0.0.eba'
				],
				['cics-bundle-gradle-1.0.0-SNAPSHOT/org.apache.aries.samples.twitter.eba-1.0.0.ebabundle',
				 'cics-bundle-gradle-1.0.0-SNAPSHOT/org.apache.aries.samples.twitter.eba-1.0.0.eba'
				],
				SUCCESS)

		checkManifest(['id="cics-bundle-gradle">',
					   '<define name="org.apache.aries.samples.twitter.eba-1.0.0" path="org.apache.aries.samples.twitter.eba-1.0.0.ebabundle" type="http://www.ibm.com/xmlns/prod/cics/bundle/EBABUNDLE"/>'
		])
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
            ${commonBuildFileContents}

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
				['Task :helloworldwar:build',
				 "${warProjectName}-1.0-SNAPSHOT.war"
				],
				['cics-bundle-gradle-1.0.0-SNAPSHOT/helloworldwar-1.0-SNAPSHOT.warbundle',
				 'cics-bundle-gradle-1.0.0-SNAPSHOT/helloworldwar-1.0-SNAPSHOT.war'
				],
				SUCCESS)

		checkManifest(['id="cics-bundle-gradle">',
		               '<define name="helloworldwar-1.0-SNAPSHOT" path="helloworldwar-1.0-SNAPSHOT.warbundle" type="http://www.ibm.com/xmlns/prod/cics/bundle/WARBUNDLE"/>'
		])
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
            ${commonBuildFileContents}
            
            dependencies {
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}(group: 'org.apache.jmeter', name: 'apache-jmeter', version: '2.3.4-atlassian-1'  )
            }
        """

		when:
		def result = runGradleAndFail()

		then:
		checkResults(result,
				[BuildBundleTask.UNSUPPORTED_EXTENSIONS_FOUND,
				 "Unsupported file extension 'gz' for dependency 'apache-jmeter-2.3.4-atlassian-1.tar.gz'"
				],
				[],
				FAILED)
	}

	def "Test no cicsBundle dependencies warning"() {

		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"

		buildFile << """\
            ${commonBuildFileContents}
           
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

	def "Test packageCICSBundle produces zip in default location"() {
		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
            ${commonBuildFileContents}

            dependencies {
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME} 'org.codehaus.cargo:simple-ear:1.7.6@ear'
            }
        """

		when:
		def result = runGradle(['packageCICSBundle'])

		then:
		checkResults(result,
				['> Task :buildCICSBundle',
				 '> Task :packageCICSBundle'
				],
				['distributions/cics-bundle-gradle-1.0.0-SNAPSHOT.zip'],
				SUCCESS)
	}

	def "Test static bundle parts and jar"() {
		given:
		copyBundlePartsToResources("static-bundle-parts")

		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
            ${commonBuildFileContents}
            
            dependencies {
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}('javax.servlet:javax.servlet-api:3.1.0@jar')
            }
        """

		when:
		def result = runGradle()

		then:
		checkResults(result,
				['javax.servlet-api-3.1.0.jar',
				 "Adding bundle part 'TCPIPSV1.tcpipservice'",
				 "Adding bundle part 'TSQAdapter.epadapter'",
				 "Adding bundle part 'TRND.transaction'",
				 "Adding bundle part 'CATMANAGER.evbind'",
				 "Adding bundle part 'LIBDEF1.library'",
				 "Adding bundle part 'URIMP011.urimap'",
				 "Adding bundle part 'PROGDEF1.program'",
				 "Adding bundle part 'FILEDEFA.file'",
				 "Adding bundle part 'PACKSET1.packageset'",
				 "Adding bundle part 'EPADSET1.epadapterset'",
				 "Adding bundle part 'TDQAdapter.epadapter'",
				 "Adding bundle part 'POLDEM1.policy'"
				],
				['cics-bundle-gradle-1.0.0-SNAPSHOT/javax.servlet-api-3.1.0_3.1.0.osgibundle'],
				SUCCESS
		)

		checkManifest([
				'id="cics-bundle-gradle">',
				'<define name="javax.servlet-api-3.1.0_3.1.0" path="javax.servlet-api-3.1.0_3.1.0.osgibundle" type="http://www.ibm.com/xmlns/prod/cics/bundle/OSGIBUNDLE"/>',
				'<define name="CATMANAGER" path="CATMANAGER.evbind" type="http://www.ibm.com/xmlns/prod/cics/bundle/EVENTBINDING"/>',
				'<define name="EPADSET1" path="EPADSET1.epadapterset" type="http://www.ibm.com/xmlns/prod/cics/bundle/EPADAPTERSET"/>',
				'<define name="FILEDEFA" path="FILEDEFA.file" type="http://www.ibm.com/xmlns/prod/cics/bundle/FILE"/>',
				'<define name="LIBDEF1" path="LIBDEF1.library" type="http://www.ibm.com/xmlns/prod/cics/bundle/LIBRARY"/>',
				'<define name="PACKSET1" path="PACKSET1.packageset" type="http://www.ibm.com/xmlns/prod/cics/bundle/PACKAGESET"/>',
				'<define name="POLDEM1" path="POLDEM1.policy" type="http://www.ibm.com/xmlns/prod/cics/bundle/POLICY"/>',
				'<define name="PROGDEF1" path="PROGDEF1.program" type="http://www.ibm.com/xmlns/prod/cics/bundle/PROGRAM"/>',
				'<define name="TCPIPSV1" path="TCPIPSV1.tcpipservice" type="http://www.ibm.com/xmlns/prod/cics/bundle/TCPIPSERVICE"/>',
				'<define name="TDQAdapter" path="TDQAdapter.epadapter" type="http://www.ibm.com/xmlns/prod/cics/bundle/EPADAPTER"/>',
				'<define name="TRND" path="TRND.transaction" type="http://www.ibm.com/xmlns/prod/cics/bundle/TRANSACTION"/>',
				'<define name="TSQAdapter" path="TSQAdapter.epadapter" type="http://www.ibm.com/xmlns/prod/cics/bundle/EPADAPTER"/>',
				'<define name="URIMP011" path="URIMP011.urimap" type="http://www.ibm.com/xmlns/prod/cics/bundle/URIMAP"/>'
		])
		checkManifestDoesNotContain(['ATOM'])
	}

	def "Test static bundle parts only"() {
		given:
		copyBundlePartsToResources("static-bundle-parts")

		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
            ${commonBuildFileContents}
        """

		when:
		def result = runGradle()

		then:
		checkResults(result,
				["Adding bundle part 'TCPIPSV1.tcpipservice'",
				 "Adding bundle part 'TSQAdapter.epadapter'",
				 "Adding bundle part 'TRND.transaction'",
				 "Adding bundle part 'CATMANAGER.evbind'",
				 "Adding bundle part 'LIBDEF1.library'",
				 "Adding bundle part 'URIMP011.urimap'",
				 "Adding bundle part 'PROGDEF1.program'",
				 "Adding bundle part 'FILEDEFA.file'",
				 "Adding bundle part 'PACKSET1.packageset'",
				 "Adding bundle part 'EPADSET1.epadapterset'",
				 "Adding bundle part 'TDQAdapter.epadapter'",
				 "Adding bundle part 'POLDEM1.policy'"
				],
				[],
				SUCCESS
		)

		checkManifest([
				'id="cics-bundle-gradle">',
				'<define name="CATMANAGER" path="CATMANAGER.evbind" type="http://www.ibm.com/xmlns/prod/cics/bundle/EVENTBINDING"/>',
				'<define name="EPADSET1" path="EPADSET1.epadapterset" type="http://www.ibm.com/xmlns/prod/cics/bundle/EPADAPTERSET"/>',
				'<define name="FILEDEFA" path="FILEDEFA.file" type="http://www.ibm.com/xmlns/prod/cics/bundle/FILE"/>',
				'<define name="LIBDEF1" path="LIBDEF1.library" type="http://www.ibm.com/xmlns/prod/cics/bundle/LIBRARY"/>',
				'<define name="PACKSET1" path="PACKSET1.packageset" type="http://www.ibm.com/xmlns/prod/cics/bundle/PACKAGESET"/>',
				'<define name="POLDEM1" path="POLDEM1.policy" type="http://www.ibm.com/xmlns/prod/cics/bundle/POLICY"/>',
				'<define name="PROGDEF1" path="PROGDEF1.program" type="http://www.ibm.com/xmlns/prod/cics/bundle/PROGRAM"/>',
				'<define name="TCPIPSV1" path="TCPIPSV1.tcpipservice" type="http://www.ibm.com/xmlns/prod/cics/bundle/TCPIPSERVICE"/>',
				'<define name="TDQAdapter" path="TDQAdapter.epadapter" type="http://www.ibm.com/xmlns/prod/cics/bundle/EPADAPTER"/>',
				'<define name="TRND" path="TRND.transaction" type="http://www.ibm.com/xmlns/prod/cics/bundle/TRANSACTION"/>',
				'<define name="TSQAdapter" path="TSQAdapter.epadapter" type="http://www.ibm.com/xmlns/prod/cics/bundle/EPADAPTER"/>',
				'<define name="URIMP011" path="URIMP011.urimap" type="http://www.ibm.com/xmlns/prod/cics/bundle/URIMAP"/>'
		])
		checkManifestDoesNotContain(['ATOM'])
	}

	def "Test resources does not exist"() {
		given:
		def resources = testProjectDir.newFolder("src", "main", "resources")
		resources.delete()

		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
            ${commonBuildFileContents}
        """

		when:
		def result = runGradle()

		then:
		checkResults(result,
				["No resources folder '${BuildBundleTask.RESOURCES_PATH}' to search for bundle parts"],
				[],
				SUCCESS
		)
	}

	def "Test resources is not a directory"() {
		given:
		def resources = testProjectDir.newFolder("src", "main", "resources")
		resources.delete()
		resources.write("I am not a folder")

		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
            ${commonBuildFileContents}
        """

		when:
		def result = runGradleAndFail()

		then:
		checkResults(result,
				["Directory '${resources.getCanonicalPath()}' specified for property 'resourcesDirectory' is not a directory."],
				[],
				FAILED
		)
	}

	def "Test rebuild static parts with changes"() {
		given:
		copyBundlePartToResources("static-bundle-parts", "FILEDEFA.file")

		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
            ${commonBuildFileContents}
        """

		when:
		def result = runGradle()

		then:
		checkResults(result,
				["Adding bundle part 'FILEDEFA.file'"],
				[],
				SUCCESS
		)

		checkManifest([
				'id="cics-bundle-gradle">',
				'<define name="FILEDEFA" path="FILEDEFA.file" type="http://www.ibm.com/xmlns/prod/cics/bundle/FILE"/>'
		])

		when:
		copyBundlePartToResources("static-bundle-parts", "LIBDEF1.library")
		result = runGradle()

		then:
		checkResults(result,
				["Adding bundle part 'FILEDEFA.file'",
				 "Adding bundle part 'LIBDEF1.library'",
				],
				[],
				SUCCESS
		)

		checkManifest([
				'id="cics-bundle-gradle">',
				'<define name="FILEDEFA" path="FILEDEFA.file" type="http://www.ibm.com/xmlns/prod/cics/bundle/FILE"/>',
				'<define name="LIBDEF1" path="LIBDEF1.library" type="http://www.ibm.com/xmlns/prod/cics/bundle/LIBRARY"/>'
		])
	}

	def "Test rebuild Java parts with changes"() {
		given:
		settingsFile << "rootProject.name = 'cics-bundle-gradle'"
		buildFile << """\
           ${commonBuildFileContents}
            
            dependencies {
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}(group: 'org.glassfish.main.admingui', name: 'war', version: '5.1.0', ext: 'war')
            }
        """

		when:
		def result = runGradle()

		then:
		checkResults(result,
				['org.glassfish.main.admingui',
				 'war-5.1.0.war'
				],
				['cics-bundle-gradle-1.0.0-SNAPSHOT/war-5.1.0.warbundle',
				 'cics-bundle-gradle-1.0.0-SNAPSHOT/war-5.1.0.war'
				],
				SUCCESS)

		checkManifest(['id="cics-bundle-gradle">',
					   '<define name="war-5.1.0" path="war-5.1.0.warbundle" type="http://www.ibm.com/xmlns/prod/cics/bundle/WARBUNDLE"/>'
		])

		when:
		buildFile << """\
            dependencies {
                ${BundlePlugin.BUNDLE_DEPENDENCY_CONFIGURATION_NAME}(group: 'org.codehaus.cargo', name: 'simple-ear', version: '1.7.6', ext: 'ear')
            }
        """
		result = runGradle()

		then:
		checkResults(result,
				['org.glassfish.main.admingui',
				 'war-5.1.0.war',
				 'org.codehaus.cargo',
				 'simple-ear-1.7.6.ear'
				],
				['cics-bundle-gradle-1.0.0-SNAPSHOT/war-5.1.0.warbundle',
				 'cics-bundle-gradle-1.0.0-SNAPSHOT/war-5.1.0.war',
				 'cics-bundle-gradle-1.0.0-SNAPSHOT/simple-ear-1.7.6.earbundle',
				 'cics-bundle-gradle-1.0.0-SNAPSHOT/simple-ear-1.7.6.ear'
				],
				SUCCESS)

		checkManifest(['id="cics-bundle-gradle">',
					   '<define name="war-5.1.0" path="war-5.1.0.warbundle" type="http://www.ibm.com/xmlns/prod/cics/bundle/WARBUNDLE"/>',
					   '<define name="simple-ear-1.7.6" path="simple-ear-1.7.6.earbundle" type="http://www.ibm.com/xmlns/prod/cics/bundle/EARBUNDLE"/>'
		])
	}
}
