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

class BundlePlugin implements Plugin<Project> {
    void apply(Project target) {
        target.tasks.register('buildCICSBundle', CICSBundleBuilderTask)
    }
}
