# Multi-part project sample (gradle-multipart-sample)
This sample shows how to configure a multi-part Gradle project to build and deploy a CICS bundle, with a separate module to contain bundle configurations. The sample contains:
* A multi-part Gradle project (`gradle-multipart-sample`) that contains the other child modules.
* A child module (`gradle-war-demo`) containing a Java project which uses JCICS, declared as a dependency from Maven Central.
* The other child module (`gradle-bundle-demo`) contains the configuration to package the `gradle-war-demo` Java project and a remote Java project (`simple-war`), also known as Java-based bundle parts, into a CICS bundle and then deploy this to CICS.

If you don't have any existing Gradle project, this sample is the quickest to try out the plugin.

# Set Up
Have your system programmer create your BUNDLE definition in CSD.
Your system programmer should create a BUNDLE definition in CSD and tell you the CSD group and BUNDLE definition name they have used. The BUNDLEDIR of the BUNDLE definition your system programmer creates should be set as follows: `<bundles-directory>/<bundle_id>_<bundle_version>`.  
So for this sample, if your system programmer configured bundles-directory as `/u/someuser/bundles/`, the BUNDLEDIR would be `/u/someuser/bundles/gradle-bundle-demo_1.0.0`.

# Using the sample
[Clone the repository](https://github.com/IBM/cics-bundle-gradle.git) and import the sample, `samples/gradle-multipart-sample`, into your IDE.

Edit the variables in the `cicsBundle` block of the `gradle-bundle-demo/build.gradle` file, to match the correct CMCI URL, CSD group, CICSplex, region and BUNDLE definition name for your environment, as well as supplying your CICS user ID and password. If you're deploying the bundle into a single region environment (SMSS), remove the `cicsplex` and `region` fields.  

When the parent project is built, all its children will also be built.

To build all modules and package the bundle parts into a zipped CICS bundle, change to the `gradle-multipart-sample` directory and run:
```
./gradlew build
```

To package and deploy the built bundle to your CICS region, run:
```
./gradlew deployCICSBundle
```  
If you run into an `unable to find valid certification path to requested target` error during deployment, uncommenting the `insecure = true` line in the bundle project's `build.gradle` is a quick fix but it poses security concerns by disabling TLS/SSL checking for certificates. For recommended solutions in real use, refer to [Troubleshooting](https://github.com/IBM/cics-bundle-gradle#troubleshooting). 

# What's next
Visit the servlet (http://myserver.site.domain.com:1234/gradle-war-demo-1.0.0) to see what you published.
