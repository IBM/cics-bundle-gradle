package com.ibm.cics.cbgp

import org.gradle.testkit.runner.GradleRunner
import org.junit.Before;
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

//import static org.gradle.testkit.runner.TaskOutcome.*

class TrialFunctionalTest {
    List<File> pluginClasspath

    @Rule public TemporaryFolder testProjectDir = new TemporaryFolder()
    File settingsFile
    File buildFile

    @Before
    void setup() {
        ExpandoMetaClass.disableGlobally()
        settingsFile = testProjectDir.newFile('settings.gradle')
        buildFile = testProjectDir.newFile('build.gradle')
     }

    @Test
    void "missing cicsBundle config prints error message"() {
//        given:
        settingsFile << "rootProject.name = 'cics-bundle-gradle'"
        buildFile << """
            plugins {
                id 'cics-bundle-gradle-plugin'
            }
            
            version '1.0.0-SNAPSHOT'

            
            repositories {
                jcenter()
                mavenCentral()
            }
            
            configurations {
                cicsBundle
            }
            
            dependencies {
//                project(path: 'buildSrc')
//                classpath("com.ibm.cics.cbgp.BundlePlugin:1.0.0-SNAPSHOT")
                cicsBundle(group: 'org.glassfish.main.admingui', name: 'war', version: '5.1.0', ext: 'war'  )
            }
        """

        printBuildFile()


//        when:
//        println("plugin classpath: $pluginClasspath"

        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('buildCICSBundle')
                .withPluginClasspath(/*pluginClasspath*/)
//                .withDebug(true)
                .build()
        println ("Test output:")
        println(result.output)

//        then:
//        assert result.output.contains('Define \'cicsBundle\' configuration with CICS bundle dependencies')
//        result.task(":buildCICSBundle").outcome == FAILED
    }

    private void printBuildFile() {
        int lineNumber = 0
        println('  Build file ----')
        buildFile.eachLine { line ->
            lineNumber++
            println "  $lineNumber : $line";
        }
        println('  -----')
    }
}
