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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class DeployTests extends Specification {
    List<File> pluginClasspath

    @Rule
    public TemporaryFolder testProjectDir = new TemporaryFolder()
    File settingsFile
    File buildFile

    def setup() {
        ExpandoMetaClass.disableGlobally()
        settingsFile = testProjectDir.newFile('settings.gradle')
        buildFile = testProjectDir.newFile('build.gradle')
    }

    def "Test missing deploy extension block"() {
        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                jcenter()
            }
            
            configurations {
                cicsBundle
            }
            
            dependencies {
                cicsBundle('javax.servlet:javax.servlet-api:3.1.0@jar')
            }
        """

        when:
        def result = runGradle('Test missing deploy extension block', ['deployCICSBundle'], true)

        then:
        checkResults(result, [DeployBundleTask.MISSING_CONFIG, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
    }

    def "Test empty deploy extension block"() {
        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                jcenter()
            }
            
            configurations {
                cicsBundle
            }
            
            deploy {
            }
            
            dependencies {
                cicsBundle('javax.servlet:javax.servlet-api:3.1.0@jar')
            }
        """

        when:
        def result = runGradle('Test empty deploy extension block', ['deployCICSBundle'], true)

        then:
        checkResults(result, [DeployBundleTask.MISSING_CONFIG, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
    }

    def "Test missing default jvm server"() {
        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                jcenter()
            }
            
            configurations {
                cicsBundle
            }
            
            deploy {
                cicsplex = 'MYPLEX'
                region = 'MYEGION'
                bunddef = 'MYDEF'
                csdgroup = 'MYGROUP'
                serverid = 'SPB // TODO define server definitions with user & password'
            }
            
            dependencies {
                cicsBundle('javax.servlet:javax.servlet-api:3.1.0@jar')
            }
        """

        when:
        def result = runGradle('Test missing default jvm server', ['deployCICSBundle'], true)

        then:
        checkResults(result, [DeployBundleTask.MISSING_JVMSERVER, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
    }

    def "Test missing cicsplex"() {
        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                jcenter()
            }
            
            configurations {
                cicsBundle
            }
            
            deploy {
                defaultjvmserver    = 'JVMSRVR'
                region = 'MYEGION'
                bunddef = 'MYDEF'
                csdgroup = 'MYGROUP'
                serverid = 'SPB // TODO define server definitions with user & password'
            }
            
            dependencies {
                cicsBundle('javax.servlet:javax.servlet-api:3.1.0@jar')
            }
        """

        when:
        def result = runGradle('Test missing cicsplex', ['deployCICSBundle'], true)

        then:
        checkResults(result, [DeployBundleTask.MISSING_CICSPLEX, DeployBundleTask.PLEASE_SPECIFY ], [], FAILED)
    }

    def "Test missing region"() {
        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                jcenter()
            }
            
            configurations {
                cicsBundle
            }
            
            deploy {
                defaultjvmserver    = 'JVMSRVR'
                cicsplex            = 'MYPLEX'
                bunddef = 'MYDEF'
                csdgroup = 'MYGROUP'
                serverid = 'SPB // TODO define server definitions with user & password'
            }
            
            dependencies {
                cicsBundle('javax.servlet:javax.servlet-api:3.1.0@jar')
            }
        """

        when:
        def result = runGradle('Test missing region', ['deployCICSBundle'], true)

        then:
        checkResults(result, [DeployBundleTask.MISSING_REGION, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
    }

    def "Test missing bunddef"() {
        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                jcenter()
            }
            
            configurations {
                cicsBundle
            }
            
            deploy {
                defaultjvmserver    = 'JVMSRVR'
                cicsplex            = 'MYPLEX'
                region              = 'MYEGION'
                csdgroup = 'MYGROUP'
                serverid = 'SPB // TODO define server definitions with user & password'
            }
            
            dependencies {
                cicsBundle('javax.servlet:javax.servlet-api:3.1.0@jar')
            }
        """

        when:
        def result = runGradle('Test missing bunddef', ['deployCICSBundle'], true)

        then:
        checkResults(result, [DeployBundleTask.MISSING_BUNDDEF, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
    }

    def "Test missing csdgroup"() {
        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                jcenter()
            }
            
            configurations {
                cicsBundle
            }
            
            deploy {
                defaultjvmserver    = 'JVMSRVR'
                cicsplex            = 'MYPLEX'
                region              = 'MYEGION'
                bunddef             = 'MYDEF'
                serverid = 'SPB // TODO define server definitions with user & password'
            }
            
            dependencies {
                cicsBundle('javax.servlet:javax.servlet-api:3.1.0@jar')
            }
        """

        when:
        def result = runGradle('Test missing csdgroup', ['deployCICSBundle'], true)

        then:
        checkResults(result, [DeployBundleTask.MISSING_CSDGROUP, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
    }

    def "Test missing serverid"() {
        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                jcenter()
            }
            
            configurations {
                cicsBundle
            }
            
            deploy {
                defaultjvmserver    = 'JVMSRVR'
                cicsplex            = 'MYPLEX'
                region              = 'MYEGION'
                bunddef             = 'MYDEF'
                csdgroup            = 'MYGROUP'
            }
            
            dependencies {
                cicsBundle('javax.servlet:javax.servlet-api:3.1.0@jar')
            }
        """

        when:
        def result = runGradle('Test missing serverid', ['deployCICSBundle'], true)

        then:
        checkResults(result, [DeployBundleTask.MISSING_SERVERID, DeployBundleTask.PLEASE_SPECIFY], [], FAILED)
    }

    def "Test multiple items missing"() {
        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                jcenter()
            }
            
            configurations {
                cicsBundle
            }
            
            deploy {
                cicsplex            = 'MYPLEX'
                csdgroup            = 'MYGROUP'
            }
            
            dependencies {
                cicsBundle('javax.servlet:javax.servlet-api:3.1.0@jar')
            }
        """

        when:
        def result = runGradle('Test multiple items missing', ['deployCICSBundle'], true)

        then:
        checkResults(result, [
                DeployBundleTask.MISSING_JVMSERVER,
                DeployBundleTask.MISSING_REGION,
                DeployBundleTask.MISSING_BUNDDEF,
                DeployBundleTask.MISSING_SERVERID,
                DeployBundleTask.PLEASE_SPECIFY
        ], [], FAILED)
    }


    // Run the gradle build with defaults and print the test output
    def runGradle(String testName, List args = ['deployCICSBundle'], boolean failExpected = false) {
        def result
        if (!failExpected) {
            result = GradleRunner.create().withProjectDir(testProjectDir.root).withArguments(args).withPluginClasspath().build()
        } else {
            result = GradleRunner.create().withProjectDir(testProjectDir.root).withArguments(args).withPluginClasspath().buildAndFail()
        }
        def title = "\n----- '$testName' output: -----"
        println(title)
        println(result.output)
        println('-' * title.length())
        println()
        return result
    }

    def checkResults(BuildResult result, List resultStrings, List outputFiles, TaskOutcome outcome) {
        resultStrings.each {
            if (!result.output.contains(it)) {
                println("Not found in build output: '$it'")
                assert (false)
            }
        }
        outputFiles.each {
            if (!getFileInBuildOutputFolder(it).exists()) {
                println("File not found in output folder: '$it'")
                assert (false)
            }
        }
        result.task(":deployCICSBundle").outcome == outcome
    }

    private File getFileInBuildOutputFolder(String fileName) {
        return new File(buildFile.parent + '/build/cics-bundle-gradle-1.0.0-SNAPSHOT/' + fileName)
    }

    // Some useful functions as I can't get debug to work for Gradle Runner tests yet
    private void printTemporaryFileTree() {
        def tempFolder = new File(buildFile.parent)

        println("  Temp file tree: $tempFolder  ----")
        tempFolder.traverse {
            println('   ' + it.path)
        }
        println('  -----')
    }

}
