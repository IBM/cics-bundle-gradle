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
`buildCICSBundle`| Builds a CICS bundle.<br/>Java-based dependencies such as OSGi bundle, WAR, EAR, or EBAs are added using the `cicsBundle` dependency configuration. You'll need to specify the default JVM server in the `cicsBundle` extension block.<br/>Resource definition bundleparts are automatically added from the src/main/resources folder of your project.
`packageCICSBundle`| Packages the built CICS bundle into a zipped archive.
`deployCICSBundle`| Deploys the packaged CICS bundle to CICS on z/OS, installs and enables it, using settings in the `cicsBundle` extension block.
`build` | Performs a full build of the project, including assembling all artifacts and running all tests. **You only need to call the `build` task when building or packaging your CICS bundles as it depends on those tasks.**

Their dependencies are as follows:
```
:build
+--- :assemble
|    \--- :packageCICSBundle
|         \--- :buildCICSBundle
\--- :check


:deployCICSBundle
\--- :packageCICSBundle
     \--- :buildCICSBundle
```

## Configure the CICS bundle Gradle plugin
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
            gradlePluginPortal()
        }
    }
    ```

## Build and package a CICS bundle
1. In your `build.gradle`, define the version for the bundle.
    ```gradle
    version '1.0.0'
    ```
1. Add Java-based dependencies to the bundle by adding them to the `dependencies` block using the `cicsBundle` configuration.
    * To include a dependency produced by the bundle project itself, e.g. when you are converting an existing Java project into a CICS bundle, use the `files` notation, and specify the name of the task which produces the bundlepart archive, e.g. `jar`, `war`, or `ear`.
        ```gradle
        dependencies {
            cicsBundle files(war)
        }
        ```
    * To include a dependency produced by a different local project, use the `project` notation with the `archives` configuration, and specify the path to the local project.
        ```gradle
        dependencies {
            cicsBundle project(path: ':path-to-other-project', configuration: 'archives')
        }
        ```
    * To include a dependency hosted in a remote repository such as Maven Central, use the default `module` notation.
        ```gradle
        dependencies {
            cicsBundle(group: 'org.codehaus.cargo', name: 'simple-war', version: '1.7.7', ext: 'war')
        }
        ```
        Then specify the repository to use to retrieve the remote dependency.
        ```gradle
        repositories {
            mavenCentral()
        }
        ```
1. If using Java-based bundleparts, add the `cicsBundle` extension block to define the default JVM server that they will use.
    ```gradle
    cicsBundle {
        defaultJVMServer = 'MYJVMS'
    }
    ```
1. To include CICS resource definition bundleparts like FILE or URIMAP, put the bundlepart files in the src/main/resources directory. Files in this directory will be included within the output CICS bundle, and supported types will have a <define> element added to the CICS bundle's cics.xml.
1. Invoke the `build` task in your build. It builds the CICS bundle with its contained bundleparts, and packages it as a zip file.

## Deploy a CICS bundle
Deploying your bundle to CICS requires extra configuration in CICS, as described in [Pre-requisites](https://github.com/IBM/cics-bundle-gradle#pre-requisites).

Also ensure a BUNDLE definition for this CICS bundle has already been created in the CSD. You can ask your system admin to do this and pass you the CSD group and name of the definition. The bundle directory of the BUNDLE definition should be set as follows to match your CICS bundle:`<bundle_deploy_root>/<bundle_id>_<bundle_version>`.

1. In the CICS bundle module's `build.gradle`, add settings to the `cicsBundle` extension block for the deploy destination.
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
