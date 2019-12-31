# cics-bundle-gradle

A collection of Gradle plugins and utilities that can be used to build CICS bundles, ready to be installed into CICS TS.

This project contains:
  The CICS bundle Gradle plugin (`com.ibm.cics.bundle`), a Gradle plugin that can build CICS bundles, include selected Java-based dependencies, and deploy them to CICS.

## Supported bundlepart types
The CICS bundle Gradle plugin supports building CICS bundles that contain the following bundleparts:
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
 If you're using the `deployCICSBundle` task, further configuration to CICS is required to make use of the CICS bundle deployment API (see the [CICS TS doc](https://www.ibm.com/support/knowledgecenter/en/SSGMCP_5.6.0/configuring/cmci/config-bundle-api.html) for details). Simply put, you'll need:  
 * A CICS region that is at CICS® TS V5.6 open beta or later
 * The region to be configured as a WUI region for the CICSplex that contains the deployment target region
 * The WUI region to be configured to use the CMCI JVM server, including the CICS bundle deployment API

## Gradle Tasks
 The CICS bundle Gradle plugin contributes the following gradle tasks in sequential order.

Tasks | Description
--|--
`buildCICSBundle`| Scopes the EAR, WAR and OSGi java dependencies to be added to the CICS bundle, using the `cicsBundle` dependency configuration. Other bundleparts are automatically added from the resources folder of your build.<br/>You'll need to specify the default JVM server in the `cicsBundle` block.
`deployCICSBundle`| Deploys the CICS bundle to CICS on z/OS, installs and enables it, using settings in the `cicsBundle` block.
`assemble` | Assembles all the archives in the project.
`build` | Performs a full build of the project. **You only need to call the `build` task when packaging or deploying your CICS bundles as it depends on previous tasks.**

Their dependencies are as follows:
```
:build
+--- :assemble
|    \--- :deployCICSBundle
|         +--- :buildCICSBundle
```

## Configure for the CICS bundle Gradle plugin
To use the plugin, clone or download the GitHub repository. Then create a separate Gradle module for your CICS bundle and configure it as follows.

1. Add the plugin id to your `build.gradle`.
    ```gradle
     plugins {
         id 'com.ibm.cics.bundle' version '0.0.1-SNAPSHOT'
     }
    ```
1. If using a snapshot version of the plugin, add the snapshot repository to your `settings.gradle`, so Gradle can find the plugin.
    ```gradle
    pluginManagement {
        repositories {
            maven {
                name = "Sonatype Snapshots"
                url = uri("https://oss.sonatype.org/content/repositories/snapshots")
            }
            mavenCentral() // Needed for the plugin's own deps
        }
    }
    ```

## Build a CICS bundle
Before building the CICS bundle module, you need to build the cloned plugin first, which provides necessary dependencies.

1. In the CICS bundle module, add local and remote dependencies to the `cicsBundle` configuration in the `dependencies` block, by prepending them
 with the `cicsBundle` build configuration name that the plugin provides.
     ```gradle
     dependencies {
          // A project within the build
         cicsBundle project(path: ':helloworldwar', configuration: 'war')

         // External dependencies, specify the repositories in the repositories block as usual
         cicsBundle(group: 'org.glassfish.main.admingui', name: 'war', version: '5.1.0', ext: 'war')
         cicsBundle(group: 'javax.servlet', name: 'javax.servlet-api', version: '3.1.0', ext: 'jar')
     }
     ```
1. Add the `cicsBundle` block to define the default JVM server used by Java bundleparts.
     ```gradle
        cicsBundle {
           defaultJVMServer = 'MYJVMS'
        }
     ```
1. Define the version information for the bundle.
     ```gradle
       version '1.0.0-SNAPSHOT'
     ```
1. Invoke the `build` task in your build. It builds the CICS bundle with its contained modules, and packages it as a zip file.


## Deploy a CICS bundle
Deploying your bundle to CICS requires extra configuration in CICS, as described in [Pre-requisites](https://github.com/IBM/cics-bundle-gradle#pre-requisites).

Also ensure a BUNDLE definition for this CICS bundle has already been created in the CSD. You can ask your system admin to do this and pass you the CSD group and name of the definition. The bundle directory of the BUNDLE definition should be set as follows to match your CICS bundle:`<bundle_deploy_root>/<bundle_id>_<bundle_version>`.

1. In the CICS bundle module's `build.gradle`, add settings to the `cicsBundle` block for the deploy destination.
      ```gradle
         cicsBundle {
             cicsplex = 'MYPLEX'
             region   = 'MYEGION'
             bunddef  = 'MYDEF'
             csdgroup = 'MYGROUP'
             url      = 'myserver.site.domain.com:1234'
             username = project.myUsername      // Define myUsername in gradle.properties file
             password = project.myPassword      // Define myPassword in gradle.properties file   
         }
    ```
    Edit the code snippet above to match your CICS configuration:
    * `url` - Set the transport, hostname, and port for your CMCI
    * `username & password` - These are your credentials for CICS. You can define them in the `gradle.properties` file and call them here, or use other plugins for further encryption, such as the [gradle-credentials-plugin](https://github.com/etiennestuder/gradle-credentials-plugin).
    * `bunddef` - The name of the BUNDLE definition to be installed.
    * `csdgroup` - The name of the CSD group that contains the BUNDLE definition.
    * `cicsplex` - The name of the CICSplex that the target region belongs to.
    * `region` - The name of the region that the bundle should be installed to.  
1. Invoke the `deployCICSBundle` task in your build to deploy the bundle to the target CICSplex and region.


## Contributing

We welcome contributions! Find out how in our [contribution guide](CONTRIBUTING.md).

## Licence

This project is licensed under the Eclipse Public License, Version 2.0.
