# Standalone project sample (gradle-war-sample)
This sample shows how you can configure an existing WAR project to build a CICS bundle. The `standalone-war-demo` directory would be your existing WAR project in a Gradle project. The WAR project is configured (in `standalone-war-demo/build.gradle`) to package its generated WAR as a bundle part into a CICS bundle and deploy the bundle to CICS.

# Set Up
Have your system programmer create your BUNDLE definition in CSD.
Your system programmer should create a BUNDLE definition in CSD and tell you the CSD group and BUNDLE definition name they have used. The BUNDLEDIR of the BUNDLE definition your system programmer creates should be set as follows: `<bundles-directory>/<bundle_id>_<bundle_version>`.  
So for this sample, if your system programmer configured bundles-directory as `/u/someuser/bundles/`, the BUNDLEDIR would be `/u/someuser/bundles/standalone-war-demo_1.0.0`.

# Using the sample
There are 2 ways to use this sample. Option 1 is to use the whole sample as-is, for example, if you want to try this out before using it with an existing Gradle project. Option 2 is to extend an existing Gradle WAR project, which you’d like to package and install as a CICS bundle.

## Option 1: import the full sample
[Clone the repository](https://github.com/IBM/cics-bundle-gradle.git) and import the sample, `samples/gradle-war-sample`, into your IDE.  

Edit the variables in the `cicsBundle` block of the `standalone-war-demo/build.gradle` file, to match the correct CMCI URL, CSD group, CICSplex, region and BUNDLE definition name for your environment, as well as supplying your CICS user ID and password.

## Option 2: add to an existing Gradle project
If you have an existing Java Gradle project, add the snippet shown below to the `cicsBundle` and `dependencies` sections of your `build.gradle` and edit the variables as needed. Your Gradle project should now resemble the sample.
```gradle
cicsBundle {
    build {
        defaultJVMServer = 'DFHWLP'
    }
    deploy {
        cicsplex = 'MYPLEX'
        region   = 'MYREGION'
        bunddef  = 'MYBUN'
        csdgroup = 'MYGROUP'
        url      = 'myserver.site.domain.com:1234'
        username = project.cicsUser    // Define cicsUser in gradle.properties file
        password = project.cicsPass   // Define cicsPass in gradle.properties file
        // Caution: uncomment the line below to disable TLS/SSL checking for certificates
        //insecure = true
    }
}
```  
```gradle
dependencies {
    cicsBundlePart files(war) //specify the name of the task which produces the bundle part archive
}
```

# Build
To build all projects and package the bundle part into a zipped CICS bundle, change to the `gradle-war-sample` directory and run:
```
./gradlew build
```

To package and deploy the built bundle to your CICS region, run:
```
./gradlew deployCICSBundle
```

If you run into an `unable to find valid certification path to requested target` error during deployment, uncommenting the `insecure = true` line in the bundle project's `build.gradle` is a quick fix but it poses security concerns by disabling TLS/SSL checking for certificates. For recommended solutions in real use, refer to [Troubleshooting](https://github.com/IBM/cics-bundle-gradle#troubleshooting).

# What's next
Visit the servlet (http://myserver.site.domain.com:1234/standalone-war-demo-1.0.0 if you used our sample as-is) to see what you published.
