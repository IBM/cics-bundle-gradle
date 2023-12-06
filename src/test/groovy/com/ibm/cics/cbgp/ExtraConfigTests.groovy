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
import spock.lang.Title
import spock.lang.Unroll

/**
 * Test bundle parts which have been explicitly configured with extra values. E.g. overrides for name, jvmserver, etc.
 */
class ExtraConfigTests extends AbstractTest {

    @Unroll
    def "Test extra config with #syntax syntax on Gradle version #gradleVersion"(String syntax, String gradleVersion) {

        given:
        rootProjectName = "extra-config-project"
        bundleProjectName = "extra-config-bundle"
        def osgiNameOverrideModule = "new-osgi-module"
        def warNameOverrideModule = "new-war-module"
        def earNameOverrideModule = "new-ear-module"
        def osgiNameOverrideProject = "new-osgi-project"
        def warNameOverrideProject = "new-war-project"
        def earNameOverrideProject = "new-ear-project"
        def osgiNameOverrideFile = "new-osgi-file"
        def warNameOverrideFile = "new-war-file"
        def earNameOverrideFile = "new-ear-file"
        def ebaNameOverrideFile = "new-eba-file"

        copyTestProject()

        // Use different build.gradle depending on syntax.
        File srcFile = getFileInDir(bundleProjectDir, "${syntax}.build.gradle")
        File destFile = buildFile
        FileUtils.copyFile(srcFile, destFile)

        def jvmserverOriginal = gradleProperties.getProperty("jvmsWlp")
        def jvmserverOverride = gradleProperties.getProperty("jvmsOsgi")

        when:
        def result = runGradleAndSucceed([BundlePlugin.DEPLOY_TASK_NAME], gradleVersion)

        then:
        checkBuildOutputStrings(result, [
                "Extra configuration found for Java-based bundle part: 'extra-config-osgi-1.0.0.jar': {name=$osgiNameOverrideProject, jvmserver=$jvmserverOverride}",
                "Extra configuration found for Java-based bundle part: 'extra-config-war-1.0.0.war': {name=$warNameOverrideProject}"
        ])

        checkBuildOutputFiles([
                "${osgiNameOverrideModule}.jar",
                "${osgiNameOverrideModule}.osgibundle",
                "${warNameOverrideModule}.war",
                "${warNameOverrideModule}.warbundle",
                "${earNameOverrideModule}.ear",
                "${earNameOverrideModule}.earbundle",
                "${osgiNameOverrideProject}.jar",
                "${osgiNameOverrideProject}.osgibundle",
                "${warNameOverrideProject}.war",
                "${warNameOverrideProject}.warbundle",
                "${earNameOverrideProject}.ear",
                "${earNameOverrideProject}.earbundle",
                "${osgiNameOverrideFile}.jar",
                "${osgiNameOverrideFile}.osgibundle",
                "${warNameOverrideFile}.war",
                "${warNameOverrideFile}.warbundle",
                "${earNameOverrideFile}.ear",
                "${earNameOverrideFile}.earbundle",
                "${ebaNameOverrideFile}.eba",
                "${ebaNameOverrideFile}.ebabundle"
        ])

        checkFileContains(getFileInDir(bundleBuildDir, "${osgiNameOverrideModule}.osgibundle"), ["<osgibundle jvmserver=\"${jvmserverOverride}\" symbolicname=\"org.codehaus.cargo.simple-bundle\" version=\"1.7.0\"/>"])
        checkFileContains(getFileInDir(bundleBuildDir, "${warNameOverrideModule}.warbundle"), ["<warbundle jvmserver=\"${jvmserverOriginal}\" symbolicname=\"${warNameOverrideModule}\"/>"])
        checkFileContains(getFileInDir(bundleBuildDir, "${earNameOverrideModule}.earbundle"), ["<earbundle jvmserver=\"${jvmserverOriginal}\" symbolicname=\"${earNameOverrideModule}\"/>"])
        checkFileContains(getFileInDir(bundleBuildDir, "${osgiNameOverrideProject}.osgibundle"), ["<osgibundle jvmserver=\"${jvmserverOverride}\" symbolicname=\"com.ibm.cics.extra-config-osgi\" version=\"1.0.0\"/>"])
        checkFileContains(getFileInDir(bundleBuildDir, "${warNameOverrideProject}.warbundle"), ["<warbundle jvmserver=\"${jvmserverOriginal}\" symbolicname=\"${warNameOverrideProject}\"/>"])
        checkFileContains(getFileInDir(bundleBuildDir, "${earNameOverrideProject}.earbundle"), ["<earbundle jvmserver=\"${jvmserverOriginal}\" symbolicname=\"${earNameOverrideProject}\"/>"])
        checkFileContains(getFileInDir(bundleBuildDir, "${osgiNameOverrideFile}.osgibundle"), ["<osgibundle jvmserver=\"${jvmserverOverride}\" symbolicname=\"com.ibm.cics.standalone-osgi\" version=\"1.0.1\"/>"])
        checkFileContains(getFileInDir(bundleBuildDir, "${warNameOverrideFile}.warbundle"), ["<warbundle jvmserver=\"${jvmserverOriginal}\" symbolicname=\"${warNameOverrideFile}\"/>"])
        checkFileContains(getFileInDir(bundleBuildDir, "${earNameOverrideFile}.earbundle"), ["<earbundle jvmserver=\"${jvmserverOriginal}\" symbolicname=\"${earNameOverrideFile}\"/>"])
        checkFileContains(getFileInDir(bundleBuildDir, "${ebaNameOverrideFile}.ebabundle"), ["<ebabundle jvmserver=\"${jvmserverOriginal}\" symbolicname=\"${ebaNameOverrideFile}\"/>"])

        checkManifest([
                "<define name=\"${osgiNameOverrideModule}\" path=\"${osgiNameOverrideModule}.osgibundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/OSGIBUNDLE\"/>",
                "<define name=\"${warNameOverrideModule}\" path=\"${warNameOverrideModule}.warbundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/WARBUNDLE\"/>",
                "<define name=\"${earNameOverrideModule}\" path=\"${earNameOverrideModule}.earbundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/EARBUNDLE\"/>",
                "<define name=\"${osgiNameOverrideProject}\" path=\"${osgiNameOverrideProject}.osgibundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/OSGIBUNDLE\"/>",
                "<define name=\"${warNameOverrideProject}\" path=\"${warNameOverrideProject}.warbundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/WARBUNDLE\"/>",
                "<define name=\"${earNameOverrideProject}\" path=\"${earNameOverrideProject}.earbundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/EARBUNDLE\"/>",
                "<define name=\"${osgiNameOverrideFile}\" path=\"${osgiNameOverrideFile}.osgibundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/OSGIBUNDLE\"/>",
                "<define name=\"${warNameOverrideFile}\" path=\"${warNameOverrideFile}.warbundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/WARBUNDLE\"/>",
                "<define name=\"${earNameOverrideFile}\" path=\"${earNameOverrideFile}.earbundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/EARBUNDLE\"/>",
                "<define name=\"${ebaNameOverrideFile}\" path=\"${ebaNameOverrideFile}.ebabundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/EBABUNDLE\"/>"
        ])

        checkBundleArchiveFile()

        // Parameterize test so the same test can be used for both Closure and Map syntax
        where:
        [gradleVersion, syntax] << GradleVersions.onAllVersions(["closure", "map"])
    }
}