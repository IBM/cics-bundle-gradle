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

/**
 * Test bundle parts which have been explicitly configured with extra values. E.g. overrides for name, jvmserver, etc.
 */
class ExtraConfigTests extends AbstractTest {

    @Unroll
    def "Test extra config with #syntax syntax"(String syntax) {

        given:
        rootProjectName = "multi-project"
        bundleProjectName = "multi-bundle"
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
        def jvmserverOverride = "EYUCMCIJ"

        // Use the multi-project but add some more files and overwrite the build.gradle.
        copyTestProject()
        File srcFile = getFileInDir(testResourcesDir, "extra-config/files")
        File destFile = getFileInDir(bundleProjectDir, "files")
        FileUtils.copyDirectory(srcFile, destFile)
        srcFile = getFileInDir(testResourcesDir, "extra-config/${syntax}.build.gradle")
        destFile = buildFile
        FileUtils.copyFile(srcFile, destFile)

        when:
        def result = runGradleAndSucceed([BundlePlugin.DEPLOY_TASK_NAME])

        then:
        checkBuildOutputStrings(result, [
                "Extra configuration found for Java-based bundle part: 'multi-osgi-1.0.0.jar': {name=$osgiNameOverrideProject, jvmserver=$jvmserverOverride}",
                "Extra configuration found for Java-based bundle part: 'multi-war-1.0.0.war': {name=$warNameOverrideProject}"
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
        syntax    | _
        "closure" | _
        "map"     | _
    }
}