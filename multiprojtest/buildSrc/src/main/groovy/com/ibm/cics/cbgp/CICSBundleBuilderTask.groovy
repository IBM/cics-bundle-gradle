package com.ibm.cics.cbgp

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskAction
import org.gradle.api.GradleException

class CICSBundleBuilderTask extends DefaultTask {

    public static final String CICS_BUNDLE_CONFIG_NAME = "cicsBundle"

    @TaskAction
    def buildCICSBundle() {

        // Find & process the configuration
        def foundConfig = false
        project.configurations.each {
            if (it.name == CICS_BUNDLE_CONFIG_NAME) {
                processCICSBundle(it)
                foundConfig = true
             }
        }

        if (!foundConfig) {
            throw new GradleException("Define \'$CICS_BUNDLE_CONFIG_NAME\' configuration with CICS bundle dependencies")
        }

    }

    def processCICSBundle( Configuration config) {
        println("Processing config")
        project.copy {
            from config
            eachFile {
                println(" Copying $it")
            }
            into "$project.buildDir/$project.name-$project.version"
        }
        config.dependencies.each {
            println(it)
        }
    }


}