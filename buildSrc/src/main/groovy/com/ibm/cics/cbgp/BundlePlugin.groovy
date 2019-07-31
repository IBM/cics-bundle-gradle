package com.ibm.cics.cbgp

import org.gradle.api.Plugin
import org.gradle.api.Project

class BundlePlugin implements Plugin<Project> {
    void apply(Project target) {
        target.tasks.register('buildCICSBundle', CICSBundleBuilderTask)
    }
}
