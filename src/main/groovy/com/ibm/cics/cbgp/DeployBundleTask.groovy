package com.ibm.cics.cbgp

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/*-
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

class DeployBundleTask extends DefaultTask {

    // Messages to share with test class
    public static final String  MISSING_CONFIG = 'Missing or empty deploy configuration'
    public static final String  MISSING_JVMSERVER = 'Specify default jvm server for deploy'
    public static final String  MISSING_CICSPLEX = 'Specify cicsplex for deploy'
    public static final String  MISSING_REGION = 'Specify region for deploy'
    public static final String  MISSING_BUNDDEF = 'Specify bundle definition name for deploy'
    public static final String  MISSING_CSDGROUP = 'Specify csd group for deploy'
    public static final String  MISSING_SERVERID = 'Specify server id for deploy'
    public static final String  PLEASE_SPECIFY = 'Please specify deploy configuration'
    public static final String  DEPLOY_CONFIG_EXCEPTION = PLEASE_SPECIFY + """\

Example:
     deploy {
        defaultjvmserver    = 'JVMSRVR'
        cicsplex            = 'MYPLEX'
        region              = 'MYEGION'
        bunddef             = 'MYDEF'
        csdgroup            = 'MYGROUP'
        serverid            = 'SPB'
    } 
      
    All items must be completed    
"""

    @Input
    def deployExtension = project.extensions.getByName('deploy')

    @TaskAction
    def deployCICSBundle() {
        println "Task deployCICSBundle"

        validateDeployExtension()
    }

    private void validateDeployExtension() {
        def blockValid = true

        if (deployExtension.defaultjvmserver.length() +
                deployExtension.cicsplex.length() +
                deployExtension.region.length() +
                deployExtension.bunddef.length() +
                deployExtension.csdgroup.length() +
                deployExtension.serverid.length() == 0) {
            println(MISSING_CONFIG)
            blockValid = false
        } else {
            // Validate block items exist, no check on content
            if (deployExtension.defaultjvmserver.length() == 0) {
                println(MISSING_JVMSERVER)
                blockValid = false
            }
            if (deployExtension.cicsplex.length() == 0) {
                println(MISSING_CICSPLEX)
                blockValid = false
            }
            if (deployExtension.region.length() == 0) {
                println(MISSING_REGION)
                blockValid = false
            }
            if (deployExtension.bunddef.length() == 0) {
                println(MISSING_BUNDDEF)
                blockValid = false
            }
            if (deployExtension.csdgroup.length() == 0) {
                println(MISSING_CSDGROUP)
                blockValid = false
            }
            if (deployExtension.serverid.length() == 0) {
                println(MISSING_SERVERID)
                blockValid = false
            } else {
                if (!validateServerDetails()) {
                    blockValid = false
                }
            }
        }

        // Throw exception if anything is wrong in the extension block
        if (!blockValid) {
            throw new GradleException(DEPLOY_CONFIG_EXCEPTION)
        }
    }

    private boolean validateServerDetails() {
        return true   // TODO Validate server block when known.
    }


}