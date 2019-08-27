package com.ibm.cics.cbgp

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

import org.gradle.api.Plugin
import org.gradle.api.Project

// Extension block for deploy details
class DeployExtension {
    String defaultjvmserver = ''
    String cicsplex = ''
    String region = ''
    String bunddef = ''
    String csdgroup = ''
    String serverid = ''
}

class BundlePlugin implements Plugin<Project> {

    public static final String DEPLOY_EXTENSION_NAME = 'deploy'
    public static final String BUILD_TASK_NAME = 'buildCICSBundle'
    public static final String DEPLOY_TASK_NAME = 'deployCICSBundle'

    void apply(Project target) {
        target.extensions.create(DEPLOY_EXTENSION_NAME, DeployExtension)
        target.tasks.register(BUILD_TASK_NAME, BuildBundleTask)
        target.tasks.register(DEPLOY_TASK_NAME, DeployBundleTask)
    }
}
