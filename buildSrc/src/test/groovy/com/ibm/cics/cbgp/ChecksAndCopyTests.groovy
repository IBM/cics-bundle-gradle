package com.ibm.cics.cbgp

import org.gradle.api.GradleException
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.*

class ChecksAndCopyTests extends Specification{
    List<File> pluginClasspath

    @Rule public TemporaryFolder testProjectDir = new TemporaryFolder()
    File settingsFile
    File buildFile

    def setup() {
        ExpandoMetaClass.disableGlobally()
        settingsFile = testProjectDir.newFile('settings.gradle')
        buildFile = testProjectDir.newFile('build.gradle')
     }

    def "Test maven central external module dependency"() {
        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """
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
        printTestOutput(result)

        then:
        assert result.output.contains('org.glassfish.main.admingui')
        assert result.output.contains('war-5.1.0.war')
        assert(getFileInBuildOutputFolder('/war-5.1.0.war').exists())
        result.task(":buildCICSBundle").outcome == SUCCESS
    }

   def "Test incorrect configuration name"()  {
        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """
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
        printTestOutput(result)

        then:
        assert result.output.contains('Define \'cicsBundle\' configuration with CICS bundle dependencies')
        result.task(":buildCICSBundle").outcome == FAILED
    }

    private void printTestOutput(BuildResult result) {
        println("Test output: ----")
        println(result.output)
        println('-----')
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
        def tempFolder = new File( buildFile.parent)

        println("  Temp file tree: $tempFolder  ----")
        tempFolder.traverse {
            println('   ' + it.path)
        }
        println('  -----')
    }
}
