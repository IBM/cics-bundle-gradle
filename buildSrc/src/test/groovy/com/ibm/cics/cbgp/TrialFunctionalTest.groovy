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
        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }
      pluginClasspath = pluginClasspathResource.readLines().collect {  new File(it) }
    }

    @Test
    void "missing cicsBundle config prints error message"() {
//        given:
        settingsFile << "rootProject.name = 'multiprojtest'"
        buildFile << """
            version '1.0.0-SNAPSHOT'
            
//            apply plugin: com.ibm.cics.cbgp.BundlePlugin
            
            repositories {
                jcenter()
                mavenCentral()
            }
            
            configurations {
                cicsBundleX
            }
            
            dependencies {
                project(path: 'buildSrc')
                classpath("com.ibm.cics.cbgp.BundlePlugin:1.0.0-SNAPSHOT")
                cicsBundleX(group: 'org.glassfish.main.admingui', name: 'war', version: '5.1.0', ext: 'war'  )
            }
        """

        printBuildFile()


//        when:
//        println("plugin classpath: $pluginClasspath"

        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('buildCICSBundle')
                .withPluginClasspath(pluginClasspath)
//                .withDebug(true)
                .build()
        println ("Test output:")
        println(result.output)

//        then:
        result.output.contains('Define \'cicsBundle\' configuration with CICS bundle dependencies')
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
