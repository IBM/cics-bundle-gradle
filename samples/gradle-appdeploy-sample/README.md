# AppDeploy WAR upload sample

This sample shows how to use the `com.ibm.cics.appdeploy` Gradle plugin to upload a WAR file directly to a Liberty JVM server in CICS, without building a CICS bundle.

It is a different route from the `com.ibm.cics.bundle` plugin samples. Use this sample if your Liberty JVM server has the `cicsts:appDeploy-1.0` feature. If you want to deploy a CICS bundle, see the [standalone project sample](../gradle-war-sample) or the [multi-part project sample](../gradle-multipart-sample).

## Prerequisites

- A Liberty JVM server in CICS with the `cicsts:appDeploy-1.0` feature enabled.
- The server's `appDeploy` element configured for the authentication mode you want to use (`basic`, `jwt`, `client-cert` or `none`), with `allowedDeployers` including your user.
- An HTTPS connection to the server.

## What the sample does

The `build.gradle` is an ordinary WAR project with the `com.ibm.cics.appdeploy` plugin added. You don't configure how the WAR is built or packaged.

Running the `uploadWarToLiberty` task does everything in order:

1. Gradle compiles the code and builds the WAR with the `war` task.
2. The `uploadWarToLiberty` task sends the WAR, plus the Liberty application XML, as a `multipart/form-data` POST to your Liberty server.

## Running the sample

Pass your credentials on the command line. Not recommended to put them in the `build.gradle`.

```sh
./gradlew uploadWarToLiberty -PcicsToken=<token>
```

This builds the WAR for you and then uploads it, so you don't need to run `war` first. The task uploads war every time you run it.

`BUILD SUCCESSFUL` means the files were delivered, not that the application started. Check its state with `GET /appdeploy/applications/<name>`; `STARTED` means it is running.

To run it against a real server, change `serverUrl` in the `build.gradle` to the `https` base URL of your Liberty JVM server, for example `https://<host>:<port>`. The plugin adds `/appdeploy/uploadApp` itself, so don't include it.

To authenticate with a user ID and password instead of a JWT, replace `bearerToken` with `userName` and `password`. To use a server with `auth="none"`, leave out all credentials.

To authenticate with a client certificate (server `auth="client-cert"`), set `clientKeyStore`, the path to a PKCS12 keystore holding the certificate and private key. Recommended to pass its password on the command line, not in the `build.gradle`, and read it with `project.findProperty`. Use `clientKeyStoreType` for a keystore that isn't PKCS12. <!-- pragma: allowlist secret -->

## Parameters

These are the settings of the `cicsAppDeploy` extension.

| Parameter | Description | Default |
|---|---|---|
| `serverUrl` | Base URL of the Liberty server. The plugin adds `/appdeploy/uploadApp` itself. | none (required) |
| `applicationXml` | Inline Liberty application XML. Used if both this and `applicationXmlLocation` are set. | none |
| `applicationXmlLocation` | Path to a file with the Liberty application XML. A relative path is resolved against the project directory. | none |
| `userName`, `password` | Basic Authentication. Set both. | none |
| `bearerToken` | JWT for the server's `jwt` mode. | none |
| `clientKeyStore` | Keystore with the client certificate and private key, for the `client-cert` mode. A relative path is resolved against the project directory. | none |
| `clientKeyStorePassword` | Password of the client keystore. It is also used for the private key. | none |
| `clientKeyStoreType` | Type of the client keystore. | `PKCS12` |
| `connectTimeout` | Time to establish the connection, in milliseconds. | `30000` |
| `readTimeout` | Time to wait for the server's response, in milliseconds. | `300000` |

The `uploadWarToLiberty` task also has a `warFile` property, the WAR to upload. It defaults to the output of the `war` task. Set it only to upload a WAR built another way:

```gradle
tasks.named('uploadWarToLiberty') {
    warFile = file('build/other/app.war')
}
```

Set the application XML with one of `applicationXml` or `applicationXmlLocation`. Credentials are optional: leave them all out for a server with `auth="none"` only when SEC=NO.

## Application XML

The Liberty application XML tells Liberty how to run the WAR. Supply it in one of two ways:

- `applicationXml`, inline in the `build.gradle` (used by this sample).
- `applicationXmlLocation`, the path to a file.

## Troubleshooting

- `401`: the user ID, password, token or certificate was rejected, or the authentication mode on the server doesn't match.
- `403`: you authenticated, but the user is not in `allowedDeployers`.
- `PKIX path building failed`: the JVM running Gradle doesn't trust the server's certificate. Add the certificate to the trust store of that JVM.
