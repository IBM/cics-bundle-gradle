# cics-bundle-gradle

A collection of Gradle plugins and utilities that can be used to build CICS bundles, ready to be installed into CICS TS.

This project contains:
 - `cics-bundle-gradle-plugin`, a Gradle plugin that builds CICS bundles, including Java-based dependencies.

 The `cics-bundle-gradle-plugin` contributes a `buildCICSBundle` task that will use the information in the 
 `build.gradle` and dependencies to create a CICS bundle, 
 ready to be stored in an artifact repository or installed into CICS.

 To use the `cics-bundle-gradle-plugin`:

**TODO - Complete this section later ...**

 1. Define a `cicsBundle` configuration in your build.gradle
 2. Add local and remote dependencies to your cicsBundle configuration
 3. Add the cics-bundle-gradle plugin to your build
 4. Add the deployCICSBundleDetails block for the deploy destination
 5. Use the `buildCICSBundle` task in your build to create the bundle
 6. Use the `deployCICSBundle` task in your build to deploy the bundle to the target cicsplex and region
 

 ```
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
     cicsBundle project(path: ':helloworldwar', configuration: 'war')
     cicsBundle(group: 'org.glassfish.main.admingui', name: 'war', version: '5.1.0', ext: 'war'  )
     cicsBundle(group: 'javax.servlet', name: 'javax.servlet-api', version: '3.1.0', ext: 'jar')
 }
 
deployCICSBundleDetails {
   cicsplex            = 'MYPLEX'
   region              = 'MYEGION'
   bunddef             = 'MYDEF'
   csdgroup            = 'MYGROUP'
   url                 = 'myserver.domain.com'
   username            = 'alicebob'
   password            = 'coiffeur-kopeck-runabout-crime'

 ```


## Contributing

We welcome contributions! Find out how in our [contribution guide](CONTRIBUTING.md).

## Licence

This project is licensed under the Eclipse Public License, Version 2.0.
