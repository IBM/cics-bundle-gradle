# Standalone project sample (gradle-osgi-sample)
This sample shows how you can configure an existing OSGi project to build a CICS bundle. The OSGi project is configured (in `build.gradle`) to package its generated OSGi bundle jar as a bundle part into a CICS bundle and deploy the bundle to CICS.

# Set Up
Have your system programmer create your BUNDLE definition in the CSD.
Your system programmer should create a BUNDLE definition in the CSD and tell you the CSD group and BUNDLE definition name they have used. The BUNDLEDIR of the BUNDLE definition your system programmer creates should be set as follows: `<bundles-directory>/<bundle_id>_<bundle_version>`.  
So for this sample, if your system programmer configured bundles-directory as `/u/someuser/bundles/`, the BUNDLEDIR would be `/u/someuser/bundles/gradle-osgi-sample_1.0.0`.


## Using this sample project
[Clone the repository](https://github.com/IBM/cics-bundle-gradle.git) and import the sample, `samples/gradle-osgi-sample`, into your IDE.  

Edit the variables in the `cicsBundle` block of the `build.gradle` file, to match the correct CMCI URL, CSD group, CICSplex, region and BUNDLE definition name for your environment, and change your configuration to supply your CICS user ID and password via `gradle.properties`. If you're deploying the bundle into a single region environment (SMSS), remove the `cicsplex` and `region` fields.

# Build
To build all projects and package the bundle part into a zipped CICS bundle, run:
```
./gradlew build
```

To package and deploy the built bundle to your CICS region, run:
```
./gradlew deployCICSBundle
```

If you run into an `unable to find valid certification path to requested target` error during deployment, uncommenting the `insecure = true` line in the bundle project's `build.gradle` is a quick fix but it poses security concerns by disabling TLS/SSL checking for certificates. For recommended solutions in real use, refer to [Troubleshooting](https://github.com/IBM/cics-bundle-gradle#troubleshooting).
