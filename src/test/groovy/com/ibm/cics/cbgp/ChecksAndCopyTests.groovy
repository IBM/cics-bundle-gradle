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

class ChecksAndCopyTests extends Specification {
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

    def "Test jcenter jar dependency"() {
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
                ${BuildBundleTask.CONFIG_NAME}
            }
            
            dependencies {
                ${BuildBundleTask.CONFIG_NAME}('javax.servlet:javax.servlet-api:3.1.0@jar')
            }
        """

        when:
        def result = runGradle('Test jcenter jar dependency')

        then:
        checkResults(result, ['javax.servlet-api-3.1.0.jar'], ['/javax.servlet-api-3.1.0.jar'], SUCCESS)
    }

    def "Test maven war dependency"() {
        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                mavenCentral()
            }
            
            configurations {
                ${BuildBundleTask.CONFIG_NAME}
            }
            
            dependencies {
                ${BuildBundleTask.CONFIG_NAME}(group: 'org.glassfish.main.admingui', name: 'war', version: '5.1.0', ext: 'war'  )
            }
        """

        when:
        def result = runGradle('Test maven war dependency')

        then:
        checkResults(result, ['org.glassfish.main.admingui', 'war-5.1.0.war'], ['/war-5.1.0.war'], SUCCESS)
    }

    def "Test maven ear dependency"() {
        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                mavenCentral()
            }
            
            configurations {
                ${BuildBundleTask.CONFIG_NAME}
            }
            
            dependencies {
                ${BuildBundleTask.CONFIG_NAME}(group: 'org.codehaus.cargo', name: 'simple-ear', version: '1.7.6', ext: 'ear'  )
            }
        """

        when:
        def result = runGradle('Test maven ear dependency')

        then:
        checkResults(result, ['org.codehaus.cargo', 'simple-ear-1.7.6.ear'], ['/simple-ear-1.7.6.ear'], SUCCESS)
    }

    def "Test incorrect configuration name"() {
        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                mavenCentral()
            }
            
            configurations {
                ${BuildBundleTask.CONFIG_NAME}X
            }
            
            dependencies {
                ${BuildBundleTask.CONFIG_NAME}X(group: 'org.glassfish.main.admingui', name: 'war', version: '5.1.0', ext: 'war'  )
            }
        """

        when:
        def result = runGradle('Test incorrect configuration name', [BundlePlugin.BUILD_TASK_NAME], true)

        then:
        checkResults(result, [BuildBundleTask.MISSING_CONFIG], [], FAILED)
    }

    def "Test local project dependency"() {

        def warProjectName = 'helloworldwar'

        given:
        settingsFile << """\
            rootProject.name = 'cics-bundle-gradle'
            include '$warProjectName'
            """
        buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                mavenCentral()
            }
            
            configurations {
                ${BuildBundleTask.CONFIG_NAME}
            }
            
            dependencies {
                ${BuildBundleTask.CONFIG_NAME} project(path: ':$warProjectName', configuration: 'war')
            }
        """

        // Copy the helloworldwar project into the test build
        def pluginClasspathResource = getClass().classLoader.findResource(warProjectName)
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find $warProjectName resource.")
        }

        def root = new File(pluginClasspathResource.path).parent
        new AntBuilder().copy(todir: (buildFile.parent + "/" + warProjectName).toString()) {
            fileset(dir: (root + "/" + warProjectName).toString())
        }

        when:
        def result = runGradle('Test local project dependency', ['build', BundlePlugin.BUILD_TASK_NAME])

        then:
        def builtWarName = "${warProjectName}-1.0-SNAPSHOT.war"
        checkResults(result, ['Task :helloworldwar:build', builtWarName], [builtWarName], SUCCESS)
    }

     def "Test incorrect dependency extension"() {

        File localBuildCacheDirectory
        localBuildCacheDirectory = testProjectDir.newFolder('local-cache')

        given:
        settingsFile << """\
            rootProject.name = 'cics-bundle-gradle'
            
            buildCache {
                local {
                    directory '${localBuildCacheDirectory.toURI().toString()}'
                }
            }
            """

        buildFile << """\
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'
            
            repositories {
                jcenter()
            }
            
            configurations {
                ${BuildBundleTask.CONFIG_NAME}
            }
            
            dependencies {
                ${BuildBundleTask.CONFIG_NAME}(group: 'org.apache.jmeter', name: 'apache-jmeter', version: '2.3.4-atlassian-1'  )
            }
        """

        when:
        def result = runGradle('Test incorrect dependency extension', [BundlePlugin.BUILD_TASK_NAME], true)

        then:
        checkResults(result, [BuildBundleTask.UNSUPPORTED_EXTENSIONS_FOUND,
                              "Unsupported file extension 'gz' for copied dependency 'apache-jmeter-2.3.4-atlassian-1.tar.gz'"]
                , [], FAILED)
    }

   def "Test no cicsBundle dependencies warning"() {

        File localBuildCacheDirectory
        localBuildCacheDirectory = testProjectDir.newFolder('local-cache')

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
                ${BuildBundleTask.CONFIG_NAME}
            }
            
            dependencies {
            }
        """

        when:
        def result = runGradle('Test no cicsBundle dependencies warning', [BundlePlugin.BUILD_TASK_NAME], false)

        then:
        checkResults(result, ["Warning, no external or project dependencies in '${BuildBundleTask.CONFIG_NAME}' configuration"], [], SUCCESS)
    }

    // Run the gradle build with defaults and print the test output
    def runGradle(String testName, List args = [BundlePlugin.BUILD_TASK_NAME], boolean failExpected = false) {
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
        result.task(":$BundlePlugin.BUILD_TASK_NAME").outcome == outcome
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
