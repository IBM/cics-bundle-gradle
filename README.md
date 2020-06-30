# CICS bundle Gradle plugin (`com.ibm.cics.bundle`)

A collection of Gradle plugins and utilities that can be used to build CICS bundles, ready to be installed into CICS TS.

This project contains:
  * The CICS bundle Gradle plugin (`com.ibm.cics.bundle`), a Gradle plugin that can build CICS bundles, include selected bundle parts, and deploy them to CICS. It's available from both the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.ibm.cics.bundle) and [Maven Central](https://search.maven.org/artifact/com.ibm.cics.bundle/com.ibm.cics.bundle.gradle.plugin).
  * `samples`, a collection of samples that show different ways of using this plugin.

## Supported bundle part types
The CICS bundle Gradle plugin supports building CICS bundles that contain the following bundle parts:

**Java-based bundle parts**
 * OSGi Bundle (JAR)
 * Web Archive (WAR)
 * Enterprise Archive (EAR)
 * Enterprise Bundle Archive (EBA)

**Non-Java-based bundle parts**
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

It can deploy CICS bundles containing any bundle parts.

## Pre-requisites
 The plugin requires Gradle version 5 features and will not work correctly on earlier releases of Gradle.  
 If you're using the `deployCICSBundle` task, further configuration to CICS is required to make use of the CICS bundle deployment API (see the [CICS TS doc](https://www.ibm.com/support/knowledgecenter/en/SSGMCP_5.6.0/configuring/cmci/config-bundle-api.html) for details). Simply put, you'll need:  
 * A CICS region that is at CICS® TS V5.6 or later
 * The region to be configured as a WUI region for the CICSplex that contains the deployment target region
 * The WUI region to be configured to use the CMCI JVM server, including the CICS bundle deployment API

## Gradle Tasks
 The CICS bundle Gradle plugin contributes the following gradle tasks in sequential order.

Tasks | Description
--|--
`buildCICSBundle`| Builds a CICS bundle.<br/>Java-based bundle parts are added using the `cicsBundlePart` dependency configuration.<br/>Non-Java-based bundle parts are automatically added from the src/main/resources folder of your project.
`packageCICSBundle`| Packages the built CICS bundle into a zipped archive.
`deployCICSBundle`| Deploys the packaged CICS bundle to CICS on z/OS, installs and enables it.
`build` | Performs a full build of the project, including assembling all artifacts and running all tests. **You only need to call the `build` task when building or packaging your CICS bundles as it depends on those tasks.**

Their dependencies are as follows:
```
:build
\--- :assemble
     \--- :packageCICSBundle
          \--- :buildCICSBundle

:deployCICSBundle
\--- :packageCICSBundle
     \--- :buildCICSBundle
```

## Configure the CICS bundle Gradle plugin
To use the plugin, you may either:
 * Add the CICS bundle configuration into an existing Gradle Java project, such as a WAR project. This will give you a single standalone project containing both the Java application and the CICS bundle configuration. The [Standalone project sample (`gradle-war-sample`)](https://github.com/IBM/cics-bundle-gradle/tree/master/samples/gradle-war-sample) shows this case.  
 * Create a separate Gradle module to contain the CICS bundle configuration. This will give you a multi-part project where the CICS bundle configuration is kept separate from the Java application. The [Multi-part project sample (`gradle-multipart-sample`)](https://github.com/IBM/cics-bundle-gradle/tree/master/samples/gradle-multipart-sample) shows this case.  

In either case, configure the Gradle module as follows:

1. Add the plugin id to your `build.gradle`.
    ```gradle
    plugins {
        id 'com.ibm.cics.bundle' version '1.0.0'
    }
    ```
1. If using a snapshot version of the plugin, add the snapshot repository to your `settings.gradle`, so Gradle can find the plugin.
    ```gradle
    pluginManagement {
        repositories {
            maven {
                name = "SonatypeSnapshots"
                url = uri("https://oss.sonatype.org/content/repositories/snapshots")
            }
            gradlePluginPortal() // Needed for the plugin's own dependencies.
        }
    }
    ```

## Build and package a CICS bundle
1. In your `build.gradle`, define the version for the bundle.
    ```gradle
    version '1.0.0'
    ```
1. Add Java-based bundle parts to the bundle by adding them to the `dependencies` block using the `cicsBundlePart` configuration.
    * If using the standalone project option: To include the bundle part produced by the project, use the `files` notation, and specify the name of the task which produces the bundle part archive, e.g. `jar`, `war`, or `ear`.
        ```gradle
        dependencies {
            cicsBundlePart files(war)
        }
        ```
    * If using the multi-part project option: To include a bundle part produced by a separate Gradle module, use the `project` notation with the `archives` configuration, and specify the path to the module.
        ```gradle
        dependencies {
            cicsBundlePart project(path: ':path-to-other-module', configuration: 'archives')
        }
        ```
    * To include a bundle part hosted in a remote repository such as Maven Central, use the default `module` notation, using any of the permitted formats.
        ```gradle
        dependencies {
            // Map notation
            cicsBundlePart group: 'org.codehaus.cargo', name: 'simple-war', version: '1.7.7', ext: 'war'
            // String notation
            cicsBundlePart 'org.codehaus.cargo:simple-war:1.7.7@war'
        }
        ```
        Then specify the repository to use to retrieve the remote bundle part.
        ```gradle
        repositories {
            mavenCentral()
        }
        ```
1. Add the `cicsBundle` extension block to your `build.gradle`. This is where you will supply additional configuration properties for the plugin. There are separate sub-blocks for `build` and `deploy` properties.
    ```gradle
    cicsBundle {
        build {
            ...
        }
        deploy {
            ...
        }
    }
    ```
1. If you have included any Java-based bundle parts, update the `cicsBundle` extension to define the default JVM server that they will use.
    ```gradle
    cicsBundle {
        build {
            defaultJVMServer = 'DFHWLP'
        }
    }
    ```
1. To include non-Java-based bundle parts, put the bundle part files in the `src/main/bundleParts` directory. Files in this directory will be automatically included in the CICS bundle, and supported types will have a <define> element added to the CICS bundle's cics.xml. The location of this directory can be configured in the `cicsBundle` extension. The configured directory is relative to `src/main/`.
    ```gradle
    cicsBundle {
        build {
            bundlePartsDirectory = 'myBundleParts'
        }
    }
    ```
1. Invoke the `build` task in your build. It builds the CICS bundle with its contained bundle parts, and packages it as a zip file.
    ```
    ./gradlew build
    ```

## Deploy a CICS bundle
Deploying your bundle to CICS requires extra configuration in CICS, as described in [Pre-requisites](https://github.com/IBM/cics-bundle-gradle#pre-requisites).

Also ensure a BUNDLE definition for this CICS bundle has already been created in the CSD. You can ask your system admin to do this and pass you the CSD group and name of the definition. The bundle directory of the BUNDLE definition should be set as follows to match your CICS bundle: `<bundle_deploy_root>/<bundle_id>_<bundle_version>`.

1. In the CICS bundle module's `build.gradle`, add settings to the `cicsBundle` extension block for the deploy destination.
    ```gradle
    cicsBundle {
        deploy {
            cicsplex = 'MYPLEX'
            region   = 'MYEGION'
            bunddef  = 'MYDEF'
            csdgroup = 'MYGROUP'
            url      = 'myserver.site.domain.com:1234'
            username = myUsername
            password = myPassword
        }
    }
    ```
    Edit the code snippet above to match your CICS configuration:
    * `url` - Set the transport, hostname, and port for your CMCI
    * `username & password` - These are your credentials for CICS. You can pass these into the build in a variety of ways (see [Gradle User Guide](https://docs.gradle.org/current/userguide/build_environment.html)), or use other plugins for further encryption, such as the [gradle-credentials-plugin](https://github.com/etiennestuder/gradle-credentials-plugin).
    * `bunddef` - The name of the BUNDLE definition to be installed.
    * `csdgroup` - The name of the CSD group that contains the BUNDLE definition.
    * `cicsplex` - The name of the CICSplex that the target region belongs to.
    * `region` - The name of the region that the bundle should be installed to.  
1. Invoke the `deployCICSBundle` task in your build to deploy the bundle to the target CICSplex and region.
    ```
    ./gradlew deployCICSBundle
    ```  
    If you run into an `unable to find valid certification path to requested target` error during deployment, see [Troubleshooting](#troubleshooting) for a fix.

## Advanced configuration
When adding Java-based bundle parts to your CICS bundle, the following defaults will be used:
* The `name` of the bundle part will be equal to the file name.
* The `jvmserver` of the bundle part will be equal to the vaule of the `defaultJVMServer` property in the `cicsBundle` extension.
* The type of the bundle part will be determined by the file extension, e.g. a `.jar` extension will result in an OSGi bundle part.

For the majority of users, these defaults will be sufficient. However, for advanced users, any of the defaults can be overriden with specific values by wrapping your `cicsBundlePart` dependency declaration inside one of the following types:
* `cicsBundleOsgi`
* `cicsBundleWar`
* `cicsBundleEar`
* `cicsBundleEba`

You may use either the closure syntax or map syntax to specify the values:
```gradle
dependencies {
    // Closure syntax
    cicsBundleWar {
        // Specify dependency as normal using any of the usual notations.
        dependency = cicsBundlePart 'org.codehaus.cargo:simple-war:1.7.7@war'
        // Changes the name from 'simple-war' to 'new-name'.
        name = 'new-name'
        // Changes the jvmserver from 'DFHWLP' to 'NEWJVMS'.
        jvmserver = 'NEWJVMS'
    }
    // Map syntax
    cicsBundleWar(dependency: cicsBundlePart('org.codehaus.cargo:simple-war:1.7.7@war'), name: 'new-name', jvmserver: 'NEWJVMS')
}
cicsBundle {
    build {
        defaultJVMServer = 'DFHWLP'
    }
}
```
 
## Samples
Use of this plugin will vary depending on what you’re starting with and the structure of your project, for example, whether you'd like to create a separate Gradle module for the bundle configuration or you'd like to include it into your existing module. We have included some samples to demonstrate the different methods.  
[Multi-part project sample (`gradle-multipart-sample`)](https://github.com/IBM/cics-bundle-gradle/tree/master/samples/gradle-multipart-sample)    
This sample is the quickest way to try the plugin out if you don't already have a Gradle project. It shows how to configure a multi-part Gradle project to build and deploy a CICS bundle, with a separate module to contain bundle configurations. The sample has a parent Gradle project that contains a local and a remote JAVA project as child modules. It also contains a CICS child module that wraps the other two modules into a CICS bundle and deploys the built bundle to CICS. A `README` is included in the sample with detailed instructions.

[Standalone project sample (`gradle-war-sample`)](https://github.com/IBM/cics-bundle-gradle/tree/master/samples/gradle-war-sample)  
If you already have a Gradle module and want to add extra configuration to it for quick use of the plugin, check out this sample. It shows you how to configure an existing WAR project to build a CICS bundle. You can either copy and paste the configuration to your WAR project or import the full sample to see how it works. A `README` is included in the sample with detailed instructions.

## Troubleshooting
### `unable to find valid certification path to requested target` during deployment
**Why does it happen?**  
You may run into this error when deploying your CICS bundle.
```
sun.security.validator.ValidatorException: PKIX path building failed:
sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```
It indicates an issue with establishing a trusted connection over TLS/SSL to the remote server (CICS bundle deployment API). It may happen when you are using a self-signed certificate or a certificate that's issued by an internal certificate authority, or that the certificate is not added to the trusted certificate list of your JVM.

**How to resolve it?**  
You have two ways of resolving this issue:
1. **Recommended** Obtain the server certificate(s) and add it/them to the trusted certificate list of your JVM:  
For security consideration, you may still want the TLS/SSL checking to be enabled. In this case, follow the instructions in [How do I import a certificate into the truststore](https://backstage.forgerock.com/knowledge/kb/article/a94909995) to trust the server's certificate, supplying your server's information. More information about the commands involved is listed below:
    * [openssl s_client](https://www.openssl.org/docs/man1.1.0/man1/openssl-s_client.html)
    * [openssl x509](https://www.openssl.org/docs/man1.1.0/man1/openssl-x509.html)
    * [Certificate encoding & extensions](https://support.ssl.com/Knowledgebase/Article/View/19/0/der-vs-crt-vs-cer-vs-pem-certificates-and-how-to-convert-them)

1. Disable TLS/SSL certificate checking:  
Add `insecure = true` to the `deploy` configuration of your bundle's `build.gradle` (See snippet in Step 1 of [Deploy a CICS bundle](#deploy-a-cics-bundle)).  
**Note:** Trusting all certificates can pose a security issue for your environment.

## Contributing

We welcome contributions! Find out how in our [contribution guide](CONTRIBUTING.md).

## Licence

This project is licensed under the Eclipse Public License, Version 2.0.
