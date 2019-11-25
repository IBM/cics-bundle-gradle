# cics-bundle-gradle

A collection of Gradle plugins and utilities that can be used to build CICS bundles, ready to be installed into CICS TS.

This project contains:
  `cics-bundle-gradle-plugin`, a Gradle plugin that builds CICS bundles, includes selected Java-based dependencies and deploys them to CICS.

## Supported bundlepart types
The `cics-bundle-gradle-plugin` supports building CICS bundles that contain the following bundleparts:
  * EAR
  * OSGi bundle
  * WAR
  * EBA
  * EPADAPTER
  * EPADAPTERSET
  * EVENTBINDING
  * FILE
  * LIBRARY
  * PACKAGESET
  * POLICY
  * PROGRAM
  * TCPIPSERVICE
  * TRANSACTION
  * URIMAP  

It can deploy CICS bundles containing any bundleparts.

## Pre-requisites
 The plugin requires Gradle version 5 features and will not work correctly on earlier releases of Gradle.  
 If you're using the `deployCICSBundle`, further configuration to CICS is required to make use of the CICS bundle deployment API (see the [CICS TS doc](https://www.ibm.com/support/knowledgecenter/en/SSGMCP_5.6.0/configuring/cmci/config-bundle-api.html) for details). Simply put, you'll need:  
 * A CICS region that is at CICS® TS V5.6 open beta or later
 * The region to be configured as a WUI region for the CICSplex that contains the deployment target region
 * The WUI region to be configured to use the CMCI JVM server, including the CICS bundle deployment API

## Gradle Tasks
 The `cics-bundle-gradle-plugin` contributes the following gradle tasks.

### `buildCICSBundle`
  This task uses the `cicsBundle` dependency configuration to scope the EAR, WAR and OSGi java dependencies to be added to the CICS bundle, other bundle parts are automatically added from the resources folder of your build.  
  Specify the default JVM server in the `buildCICSBundleConfig` block.

### `deployCICSBundle`
 This task uses settings in the `deployCICSBundleConfig` block to deploy the CICS bundle to CICS on z/OS,
 define the bundle in CICS, install and enable it.

## To use the `cics-bundle-gradle-plugin`
To use the plugin, clone or download the GitHub repository. Then create a separate Gradle module for your CICS bundle and configure it as follows.

1. Add the plugin id to your `build.gradle.kts`.
    ```gradle
     plugins {
         id 'cics-bundle-gradle-plugin'
     }
    ```
1. Add the `mavenCentral` repository to your `build.gradle.kts`, so Gradle can find the plugin.
    ```gradle
     repositories {
         mavenCentral()
     }
    ```

## To build a CICS bundle
Before building the CICS bundle module, you need to build the cloned plugin first, which provides necessary dependencies.

1. In the CICS bundle module, add local and remote dependencies to the `cicsBundle` configuration in the `dependencies` block, by prepending them
 with the `cicsBundle` build configuration name that the plugin provides.
     ```gradle
     dependencies {
          // A project within the build
         cicsBundle project(path: ':helloworldwar', configuration: 'war')

         // External dependencies, specify the repositories in the repositories block as usual
         cicsBundle(group: 'org.glassfish.main.admingui', name: 'war', version: '5.1.0', ext: 'war'  )
         cicsBundle(group: 'javax.servlet', name: 'javax.servlet-api', version: '3.1.0', ext: 'jar')
     }
     ```
1. Add the buildCICSBundleConfig block to define the default JVM server.
     ```gradle
        buildCICSBundleConfig {
           defaultjvmserver = 'EYUCMCIJ'
        }
     ```
1. Define the version information for the bundle.
     ```gradle
       version '1.0.0-SNAPSHOT'
     ```
1. Invoke the `buildCICSBundle` task in your build. It builds the CICS bundle with its contained modules.


 ## To deploy a CICS bundle

1. In the CICS bunlde module's `build.gradle.kts`, add the deployCICSBundleConfig block for the deploy destination.
      ```gradle
         deployCICSBundleConfig {
             cicsplex = 'MYPLEX'
             region   = 'MYEGION'
             bunddef  = 'MYDEF'
             csdgroup = 'MYGROUP'
             url      = 'myserver.site.domain.com:1234'
             username = my_username      // Define my_username in gradle.properties file
             password = my_password      // Define my_password in gradle.properties file   
         }
    ```

1. Invoke the `deployCICSBundle` task in your build to deploy the bundle to the target CICSplex and region.


## Contributing

We welcome contributions! Find out how in our [contribution guide](CONTRIBUTING.md).

## Licence

This project is licensed under the Eclipse Public License, Version 2.0.
