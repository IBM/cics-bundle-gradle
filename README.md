# cics-bundle-gradle

A collection of Gradle plugins and utilities that can be used to build CICS bundles, ready to be installed into CICS TS.

This project contains:
  `cics-bundle-gradle-plugin`, a Gradle plugin that builds CICS bundles, includes selected Java-based dependencies and 
 deploys them to CICS 

 The `cics-bundle-gradle-plugin` contributes the following gradle tasks.
  
### `buildCICSBundle`
  This task uses the `cicsBundle` dependency configuration to scope the EAR, WAR and OSGi java dependencies to be added 
  to the CICS bundle, other bundle parts are automatically added from the resources folder of your build.  
  Specify the default JVM server in the `buildCICSBundleConfig` block.
  
### `deployCICSBundle`
 This task uses settings in the `deployCICSBundleConfig` block to deploy the CICS bundle to CICS on z/OS, 
 define the bundle in CICS, install and enable it.
 
## To use the `cics-bundle-gradle-plugin` 
1. Add the plugin id to your build.gradle
    ```gradle
     plugins {
         id 'cics-bundle-gradle-plugin'
     }
    ```
2. Add the `mavenCentral` repository to your build.gradle, so Gradle can find the plugin 
    ```gradle
     repositories {
         mavenCentral()
     }
    ```

## To build a CICS bundle
1. Define a `cicsBundle` configuration in your build.gradle to scope the EAR, WAR and OSGi dependencies
 to be included in the bundle
    ```gradle
     configurations {
         cicsBundle
     }
    ```
 2. Add local and remote dependencies to your `cicsBundle` configuration in the `dependencies` block, by prepending them 
 with the `cicsBundle` build configuration name.
     ```gradle
     dependencies {
          // A project within the build
         cicsBundle project(path: ':helloworldwar', configuration: 'war')
         
         // External dependencies, specify the repositories in the repositories block as usual
         cicsBundle(group: 'org.glassfish.main.admingui', name: 'war', version: '5.1.0', ext: 'war'  )
         cicsBundle(group: 'javax.servlet', name: 'javax.servlet-api', version: '3.1.0', ext: 'jar')
     }
     ```
 3. Add the buildCICSBundleConfig block to define the default JVM server
     ```gradle
        buildCICSBundleConfig {
           defaultjvmserver = 'EYUCMCIJ'
        } 
     ```
 4. Define the version information for the bundle
     ```gradle
       version '1.0.0-SNAPSHOT'
     ```
 5. Invoke the `buildCICSBundle` task in your build.

 
 ## To deploy a CICS bundle
 
 1. Add the deployCICSBundleConfig block for the deploy destination
      ```gradle
         deployCICSBundleConfig {
             cicsplex = 'MYPLEX'
             region   = 'MYEGION'
             bunddef  = 'MYDEF'
             csdgroup = 'MYGROUP'
             url      = 'myserver.site.domain.com:1234'
             username = '$user'
             password = '$password'
         }
    ```

 2. Invoke the `deployCICSBundle` task in your build to deploy the bundle to the target cicsplex and region
 

## Contributing

We welcome contributions! Find out how in our [contribution guide](CONTRIBUTING.md).

## Licence

This project is licensed under the Eclipse Public License, Version 2.0.
