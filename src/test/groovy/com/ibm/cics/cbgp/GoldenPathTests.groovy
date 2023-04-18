/*
 * #%L
 * CICS Bundle Gradle Plugin
 * %%
 * Copyright (C) 2019, 2023 IBM Corp.
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
 * Test golden path scenarios where valid bundles build successfully.
 */
class GoldenPathTests extends AbstractTest {

	def "Test empty bundle"() {

		given:
		rootProjectName = bundleProjectName = "empty"

		copyTestProject()

		when:
		def result = runGradleAndSucceed([BundlePlugin.DEPLOY_TASK_NAME])

		then:
		checkBuildOutputStrings(result, [
				"Task :buildCICSBundle",
				"Task :packageCICSBundle",
				"No Java-based bundle parts found in 'cicsBundlePart' dependency configuration",
				"No non-Java-based bundle parts to add, because resources directory 'src/main/bundleParts' does not exist"
		])

		checkBuildOutputFiles(["META-INF/cics.xml"])

		checkManifest(["<manifest xmlns=\"http://www.ibm.com/xmlns/prod/cics/bundle\" bundleMajorVer=\"1\" bundleMicroVer=\"0\" bundleMinorVer=\"0\" bundleRelease=\"0\" bundleVersion=\"1\" id=\"$bundleProjectName\">"])

		checkBundleArchiveFile()
	}

	def "Test version number"() {

		given:
		rootProjectName = bundleProjectName = "version"
		projectVersion = "1.2.3"

		copyTestProject()

		when:
		runGradleAndSucceed([BundlePlugin.DEPLOY_TASK_NAME])

		then:
		checkBundleArchiveFile()
	}

	def "Test OSGi with version range"() {

		given:
		rootProjectName = bundleProjectName = "bundle-osgi-versionrange"
		projectVersion = "1.0.0"

		def jvmsOsgi = "DFHJVMS"
		def bindingExtension = "osgibundle"
		def versionRange = "[1.0.0,2.0.0)"
		def subProjectName = "multi-osgi"

		copyTestProject()

		when:
		runGradleAndSucceed([BundlePlugin.DEPLOY_TASK_NAME])

		then:
		checkFileContains(getFileInDir(bundleBuildDir, "${subProjectName + "-" + projectVersion}.${bindingExtension}") , ["<${bindingExtension} jvmserver=\"${jvmsOsgi}\" symbolicname=\"com.ibm.cics.multi-osgi\" version=\"\" versionRange=\"${versionRange}\"/>"])
		checkBundleArchiveFile()
	}
	def "Test multiple OSGi with version range"() {

		given:
		rootProjectName = bundleProjectName = "bundle-osgi-versionrange-multi"
		projectVersion = "1.0.0"

		def jvmsOsgi = "DFHJVMS"
		def bindingExtension = "osgibundle"
		def versionRange = "[1.0.0,2.0.0)"
		def versionRangeTwo = "[1.0.0,3.0.0)"
		def subProjectName = "multi-osgi"
		def subProjectNameTwo = "multi-osgi-repeat"

		copyTestProject()

		when:
		runGradleAndSucceed([BundlePlugin.DEPLOY_TASK_NAME])

		then:
		checkFileContains(getFileInDir(bundleBuildDir, "${subProjectName + "-" + projectVersion}.${bindingExtension}") , ["<${bindingExtension} jvmserver=\"${jvmsOsgi}\" symbolicname=\"com.ibm.cics.multi-osgi\" version=\"\" versionRange=\"${versionRange}\"/>"])
		checkFileContains(getFileInDir(bundleBuildDir, "${subProjectNameTwo + "-" + projectVersion}.${bindingExtension}") , ["<${bindingExtension} jvmserver=\"${jvmsOsgi}\" symbolicname=\"com.ibm.cics.multi-osgi-repeat\" version=\"\" versionRange=\"${versionRangeTwo}\"/>"])
		checkBundleArchiveFile()
	}


	@Unroll
	def "Test standalone #type project"(String type) {

		def projectName = null
		def archiveExtension = null
		def bindingExtension = null
		switch(type) {
			case "osgi":
				projectName = "standalone-osgi"
				archiveExtension = "jar"
				bindingExtension = "osgibundle"
				break
			case "war":
				projectName = "standalone-war"
				archiveExtension = "war"
				bindingExtension = "warbundle"
				break
			case "ear":
				projectName = "standalone-ear"
				archiveExtension = "ear"
				bindingExtension = "earbundle"
				break
			default:
				assert false : "Unsupported standalone type: $type"
		}

		given:
		rootProjectName = bundleProjectName = projectName

		copyTestProject()
		def jvmsWlp = gradleProperties.getProperty("jvmsWlp")
		def jvmsOsgi = gradleProperties.getProperty("jvmsOsgi")

		when:
		runGradleAndSucceed([BundlePlugin.DEPLOY_TASK_NAME])

		then:
		checkBuildOutputFiles([
				"${bundleNameAndVersion}.${archiveExtension}",
				"${bundleNameAndVersion}.${bindingExtension}"
		])

        if (type == "osgi") {
            checkFileContains(getFileInDir(bundleBuildDir, "${bundleNameAndVersion}.${bindingExtension}") , ["<${bindingExtension} jvmserver=\"${jvmsOsgi}\" symbolicname=\"com.ibm.cics.standalone-osgi\" version=\"1.0.0\"/>"])
        } else {
            checkFileContains(getFileInDir(bundleBuildDir, "${bundleNameAndVersion}.${bindingExtension}") , ["<${bindingExtension} jvmserver=\"${jvmsWlp}\" symbolicname=\"${bundleNameAndVersion}\"/>"])
        }

		checkManifest([
				"<define name=\"${bundleNameAndVersion}\" path=\"${bundleNameAndVersion}.${bindingExtension}\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/${bindingExtension.toUpperCase()}\"/>"
		])

		checkBundleArchiveFile()

		// Parameterize test so the same test can be used for each project type
		where:
		type   | _
		"osgi" | _
		"war"  | _
		"ear"  | _
	}

	def "Test multi part project"() {

		given:
		rootProjectName = "multi-project"
		bundleProjectName = "multi-bundle"
		def localOsgiNameAndVersion = "multi-osgi-${projectVersion}"
		def localWarNameAndVersion = "multi-war-${projectVersion}"
		def localEarNameAndVersion = "multi-ear-${projectVersion}"
		def remoteOsgiNameAndVersion = "simple-bundle-1.7.0"
		def remoteWarNameAndVersion = "simple-war-1.7.0"
		def remoteEarNameAndVersion = "simple-ear-1.7.0"
		def remoteEbaNameAndVersion = "org.apache.aries.samples.twitter.eba-1.0.0"

		copyTestProject()
		def jvmsWlp = gradleProperties.getProperty("jvmsWlp")
		def jvmsOsgi = gradleProperties.getProperty("jvmsOsgi")

		when:
		runGradleAndSucceed([BundlePlugin.DEPLOY_TASK_NAME])

		then:
		checkBuildOutputFiles([
				"${localOsgiNameAndVersion}.jar",
				"${localOsgiNameAndVersion}.osgibundle",
				"${localWarNameAndVersion}.war",
				"${localWarNameAndVersion}.warbundle",
				"${localEarNameAndVersion}.ear",
				"${localEarNameAndVersion}.earbundle",
				"${remoteOsgiNameAndVersion}.jar",
				"${remoteOsgiNameAndVersion}.osgibundle",
				"${remoteWarNameAndVersion}.war",
				"${remoteWarNameAndVersion}.warbundle",
				"${remoteEarNameAndVersion}.ear",
				"${remoteEarNameAndVersion}.earbundle",
				"${remoteEbaNameAndVersion}.eba",
				"${remoteEbaNameAndVersion}.ebabundle"
		])

		checkFileContains(getFileInDir(bundleBuildDir, "${localOsgiNameAndVersion}.osgibundle"), ["<osgibundle jvmserver=\"${jvmsOsgi}\" symbolicname=\"com.ibm.cics.multi-osgi\" version=\"1.0.0\"/>"])
		checkFileContains(getFileInDir(bundleBuildDir, "${localWarNameAndVersion}.warbundle"), ["<warbundle jvmserver=\"${jvmsWlp}\" symbolicname=\"${localWarNameAndVersion}\"/>"])
		checkFileContains(getFileInDir(bundleBuildDir, "${localEarNameAndVersion}.earbundle"), ["<earbundle jvmserver=\"${jvmsWlp}\" symbolicname=\"${localEarNameAndVersion}\"/>"])
		checkFileContains(getFileInDir(bundleBuildDir, "${remoteOsgiNameAndVersion}.osgibundle"), ["<osgibundle jvmserver=\"${jvmsOsgi}\" symbolicname=\"org.codehaus.cargo.simple-bundle\" version=\"1.7.0\"/>"])
		checkFileContains(getFileInDir(bundleBuildDir, "${remoteWarNameAndVersion}.warbundle"), ["<warbundle jvmserver=\"${jvmsWlp}\" symbolicname=\"${remoteWarNameAndVersion}\"/>"])
		checkFileContains(getFileInDir(bundleBuildDir, "${remoteEarNameAndVersion}.earbundle"), ["<earbundle jvmserver=\"${jvmsWlp}\" symbolicname=\"${remoteEarNameAndVersion}\"/>"])
		checkFileContains(getFileInDir(bundleBuildDir, "${remoteEbaNameAndVersion}.ebabundle"), ["<ebabundle jvmserver=\"${jvmsWlp}\" symbolicname=\"${remoteEbaNameAndVersion}\"/>"])

		checkManifest([
				"<define name=\"${localOsgiNameAndVersion}\" path=\"${localOsgiNameAndVersion}.osgibundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/OSGIBUNDLE\"/>",
				"<define name=\"${localWarNameAndVersion}\" path=\"${localWarNameAndVersion}.warbundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/WARBUNDLE\"/>",
				"<define name=\"${localEarNameAndVersion}\" path=\"${localEarNameAndVersion}.earbundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/EARBUNDLE\"/>",
				"<define name=\"${remoteOsgiNameAndVersion}\" path=\"${remoteOsgiNameAndVersion}.osgibundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/OSGIBUNDLE\"/>",
				"<define name=\"${remoteWarNameAndVersion}\" path=\"${remoteWarNameAndVersion}.warbundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/WARBUNDLE\"/>",
				"<define name=\"${remoteEarNameAndVersion}\" path=\"${remoteEarNameAndVersion}.earbundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/EARBUNDLE\"/>",
				"<define name=\"${remoteEbaNameAndVersion}\" path=\"${remoteEbaNameAndVersion}.ebabundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/EBABUNDLE\"/>"
		])

		checkBundleArchiveFile()
	}

	def "Test resources"() {

		given:
		rootProjectName = bundleProjectName = "standalone-resources"

		copyTestProject()

		when:
		runGradleAndSucceed([BundlePlugin.DEPLOY_TASK_NAME])

		then:
		checkBuildOutputFiles([
				"ATOM.xml",
				"CATMANAGER.evbind",
				"EPADSET1.epadapterset",
				"FILEDEFA.file",
				"LIBDEF1.library",
				"PACKSET1.packageset",
				"POLDEM1.policy",
				"PROGDEF1.program",
				"TCPIPSV1.tcpipservice",
				"TDQAdapter.epadapter",
				"TRND.transaction",
				"TSQAdapter.epadapter",
				"URIMP011.urimap"
		])

		checkFileContains(getFileInDir(bundleBuildDir, "PROGDEF1.program"), ["<?xml version=\"1.0\" encoding=\"UTF-8\"?><cicsdefinitionprogram xmlns=\"http://www.ibm.com/xmlns/prod/CICS/smw2int\" description=\"Demo program definition\" jvm=\"NO\" name=\"PROGDEF1\"/>"])

		checkManifest([
				"<define name=\"FILEDEFA\" path=\"FILEDEFA.file\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/FILE\"/>",
				"<define name=\"LIBDEF1\" path=\"LIBDEF1.library\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/LIBRARY\"/>",
				"<define name=\"PROGDEF1\" path=\"PROGDEF1.program\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/PROGRAM\"/>",
				"<define name=\"TRND\" path=\"TRND.transaction\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/TRANSACTION\"/>",
				"<define name=\"TDQAdapter\" path=\"TDQAdapter.epadapter\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/EPADAPTER\"/>",
				"<define name=\"TSQAdapter\" path=\"TSQAdapter.epadapter\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/EPADAPTER\"/>",
				"<define name=\"EPADSET1\" path=\"EPADSET1.epadapterset\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/EPADAPTERSET\"/>",
				"<define name=\"CATMANAGER\" path=\"CATMANAGER.evbind\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/EVENTBINDING\"/>",
				"<define name=\"PACKSET1\" path=\"PACKSET1.packageset\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/PACKAGESET\"/>",
				"<define name=\"POLDEM1\" path=\"POLDEM1.policy\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/POLICY\"/>",
				"<define name=\"TCPIPSV1\" path=\"TCPIPSV1.tcpipservice\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/TCPIPSERVICE\"/>",
				"<define name=\"URIMP011\" path=\"URIMP011.urimap\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/URIMAP\"/>"
		])

		checkBundleArchiveFile()

		// Add config to use a different location for bundle parts
		when:
		buildFile << """
		cicsBundle {
			build {
				bundlePartsDirectory = 'bundleParts2'
			}
		}
		""".stripIndent()

		runGradleAndSucceed([BundlePlugin.DEPLOY_TASK_NAME])

		then:
		checkBuildOutputFiles(["PROGDEF2.program"])
	}
}