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

class OverrideTests extends AbstractTest {

    def "Test overrides"() {

        given:
        rootProjectName = bundleProjectName = "override"
        def osgiNameOverride = "new-osgi"
        def warNameOverride = "new-war"
        def earNameOverride = "new-ear"
        def ebaNameOverride = "new-eba"
        def jvmserverOverride = "NEWJVMS"

        copyTestProject()

        when:
        runGradleAndSucceed([BundlePlugin.DEPLOY_TASK_NAME])

        then:
        checkBuildOutputFiles([
                "${osgiNameOverride}.jar",
                "${osgiNameOverride}.osgibundle",
                "${warNameOverride}.war",
                "${warNameOverride}.warbundle",
                "${earNameOverride}.ear",
                "${earNameOverride}.earbundle",
                "${ebaNameOverride}.eba",
                "${ebaNameOverride}.ebabundle"
        ])

        checkManifest([
                "<define name=\"${osgiNameOverride}\" path=\"${osgiNameOverride}.osgibundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/OSGIBUNDLE\"/>",
                "<define name=\"${warNameOverride}\" path=\"${warNameOverride}.warbundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/WARBUNDLE\"/>",
                "<define name=\"${earNameOverride}\" path=\"${earNameOverride}.earbundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/EARBUNDLE\"/>",
                "<define name=\"${ebaNameOverride}\" path=\"${ebaNameOverride}.ebabundle\" type=\"http://www.ibm.com/xmlns/prod/cics/bundle/EBABUNDLE\"/>"
        ])

        checkBundleArchiveFile()
    }
}