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
                cicsBundle
            }
            
            dependencies {
                cicsBundle('javax.servlet:javax.servlet-api:3.1.0@jar')
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('buildCICSBundle')
                .withPluginClasspath()
                .build()
        printTestOutput(result,"Test jcenter jar dependency")

        then:
        assert result.output.contains('javax.servlet-api-3.1.0.jar')
        assert (getFileInBuildOutputFolder('/javax.servlet-api-3.1.0.jar').exists())
        result.task(":buildCICSBundle").outcome == SUCCESS
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
                cicsBundle
            }
            
            dependencies {
                cicsBundle(group: 'org.glassfish.main.admingui', name: 'war', version: '5.1.0', ext: 'war'  )
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('buildCICSBundle')
                .withPluginClasspath()
                .build()
        printTestOutput(result,"Test maven war dependency")

        then:
        assert result.output.contains('org.glassfish.main.admingui')
        assert result.output.contains('war-5.1.0.war')
        assert (getFileInBuildOutputFolder('/war-5.1.0.war').exists())
        result.task(":buildCICSBundle").outcome == SUCCESS
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
                cicsBundle
            }
            
            dependencies {
                cicsBundle(group: 'org.codehaus.cargo', name: 'simple-ear', version: '1.7.6', ext: 'ear'  )
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('buildCICSBundle')
                .withPluginClasspath()
                .build()
        printTestOutput(result,"Test maven ear dependency")

        then:
        assert result.output.contains('org.codehaus.cargo')
        assert result.output.contains('simple-ear-1.7.6.ear')
        assert (getFileInBuildOutputFolder('/simple-ear-1.7.6.ear').exists())
        result.task(":buildCICSBundle").outcome == SUCCESS
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
                CICSBundle
            }
            
            dependencies {
                CICSBundle(group: 'org.glassfish.main.admingui', name: 'war', version: '5.1.0', ext: 'war'  )
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('buildCICSBundle')
                .withPluginClasspath()
                .buildAndFail()
        printTestOutput(result,"Test incorrect configuration name")

        then:
        assert result.output.contains('Define \'cicsBundle\' configuration with CICS bundle dependencies')
        result.task(":buildCICSBundle").outcome == FAILED
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
                cicsBundle
            }
            
            dependencies {
                cicsBundle project(path: ':$warProjectName', configuration: 'war')
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
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build','buildCICSBundle')  // Separate strings for each task!
                .withPluginClasspath()
                .build()
        printTestOutput(result,"Test local project dependency")

        then:
        def builtWarName = "${warProjectName}-1.0-SNAPSHOT.war"
        assert result.output.contains('Task :helloworldwar:build')
        assert result.output.contains(builtWarName)
        assert (getFileInBuildOutputFolder(builtWarName).exists())
        result.task(":buildCICSBundle").outcome == SUCCESS
    }

    def "Test incorrect dependency extension"() {
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
                cicsBundle(group: 'org.apache.jmeter', name: 'apache-jmeter', version: '2.3.4-atlassian-1'  )
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('buildCICSBundle')
                .withPluginClasspath()
                .buildAndFail()
        printTestOutput(result,"Test incorrect dependency extension")

        then:
        assert result.output.contains('Unsupported file extensions for some dependencies')
        assert result.output.contains("Invalid file extension 'gz' for copied dependency 'apache-jmeter-2.3.4-atlassian-1.tar.gz'")
        result.task(":buildCICSBundle").outcome == FAILED
    }

    private void printTestOutput(BuildResult result, String testname) {
        def title = "\n----- '$testname' output: -----"
        println(title)
        println(result.output)
        println('-' * title.length())
        println()
    }

    private File getFileInBuildOutputFolder(String fileName) {
        return new File(buildFile.parent + '/build/cics-bundle-gradle-1.0.0-SNAPSHOT/' + fileName)
    }

    // Some useful functions as I can't get debug to work for Gradle Runner tests yet
    private void printBuildFile() {
        int lineNumber = 0
        println('  Build file: ----')
        buildFile.eachLine { line ->
            lineNumber++
            println "  $lineNumber : $line";
        }
        println('  -----')
    }

    private void printTemporaryFileTree() {
        def tempFolder = new File(buildFile.parent)

        println("  Temp file tree: $tempFolder  ----")
        tempFolder.traverse {
            println('   ' + it.path)
        }
        println('  -----')
    }

}
