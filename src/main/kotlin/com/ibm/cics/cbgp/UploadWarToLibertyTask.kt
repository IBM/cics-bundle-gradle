/*-
 * #%L
 * CICS Bundle Gradle Plugin
 * %%
 * Copyright (C) 2026 IBM Corp.
 * %%
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * #L%
 */
package com.ibm.cics.cbgp

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

/**
 * Thrown for a 3xx or 4xx response.
 */
private class RejectedUploadException(message: String) : GradleException(message)

/**
 * Uploads a WAR file to a Liberty JVM server that has the cicsts:appDeploy-1.0 feature, using
 * multipart/form-data. Sends the WAR binary and the application XML as separate named parts.
 * It behaves like the upload-war goal of cics-appdeploy-maven-plugin.
 */
@UntrackedTask(because = "Upload task always needs to run to ensure the WAR is uploaded to the server")
open class UploadWarToLibertyTask : DefaultTask() {

	companion object {
		const val MAX_RETRY_ATTEMPTS = 3
		const val RETRY_DELAY_MS = 1000L
		const val BUFFER_SIZE = 8192

		/** Path of the AppDeploy upload endpoint. It is appended to the configured serverUrl. */
		const val UPLOAD_PATH = "/appdeploy/uploadApp"

		const val MISSING_SERVER_URL = "Specify serverUrl for Liberty WAR upload"
		const val MISSING_APPLICATION_XML = "Specify either applicationXml or applicationXmlLocation for Liberty WAR upload"

		val UPLOAD_CONFIG_EXCEPTION = """
			Please specify Liberty WAR upload configuration in build.gradle.

			Example with inline applicationXml and Basic Authentication:
				${AppDeployPlugin.EXTENSION_NAME} {
					serverUrl = 'https://localhost:9443'
					applicationXml = '<application id="myapp" location="myapp.war" type="war">...</application>'
					userName = 'username'
					password = 'password'
				}

			Example with applicationXmlLocation and JWT Token:
				${AppDeployPlugin.EXTENSION_NAME} {
					serverUrl = 'https://localhost:9443'
					applicationXmlLocation = 'src/main/liberty/application.xml'
					bearerToken = 'your-jwt-token'
				}

			Example with a client certificate:
				${AppDeployPlugin.EXTENSION_NAME} {
					serverUrl = 'https://localhost:9443'
					applicationXmlLocation = 'src/main/liberty/application.xml'
					clientKeyStore = 'client.p12'
					clientKeyStorePassword = project.findProperty('keystorePass')
				}

			Example with No Security (requires auth="none" on the server):
				${AppDeployPlugin.EXTENSION_NAME} {
					serverUrl = 'https://localhost:9443'
					applicationXmlLocation = 'src/main/liberty/application.xml'
					// No userName, password, bearerToken or clientKeyStore - sends request without authentication
				}
			""".trimIndent()
	}

	init {
		// Never skip the upload task, even if nothing has changed, because we can't know whether the server still has the application.
		outputs.upToDateWhen { false }
	}

	@Internal
	val cicsAppDeploy = project.extensions.getByName(AppDeployPlugin.EXTENSION_NAME) as LibertyWarUploadExtension

	@Internal
	val projectDir: File = project.projectDir

	/**
	 * The WAR file to upload. When the war plugin is applied it defaults to the output of the war task.
	 */
	@Internal
	val warFile: RegularFileProperty = project.objects.fileProperty()

	@TaskAction
	fun uploadWar() {
		logger.lifecycle("=== Upload WAR to Liberty ===")

		validateConfiguration()
		val clientSocketFactory = buildClientSocketFactory()

		val war = validateWarFile()
		logUploadInfo(war)

		try {
			uploadWarWithRetry(war, clientSocketFactory)
			logger.lifecycle("\u2713 WAR file uploaded successfully!")
		} catch (e: Exception) {
			throw GradleException("Failed to upload WAR file: ${e.message}", e)
		}
	}

	/**
	 * Validates that the WAR file exists and has a .war extension.
	 */
	private fun validateWarFile(): File {
		val war = warFile.orNull?.asFile
		if (war == null || !war.isFile) {
			throw GradleException(
				"No WAR file found. Apply the war plugin to build a WAR, or set warFile to the WAR to upload"
			)
		}
		val name = war.name
		if (!name.lowercase().endsWith(".war")) {
			val ext = if (name.contains(".")) name.substring(name.lastIndexOf('.')) else "(no extension)"
			throw GradleException("Unsupported file type: $ext. Only .war files are accepted")
		}
		return war
	}

	/**
	 * Logs upload information including file name, size, and target server.
	 */
	private fun logUploadInfo(war: File) {
		val fileSizeMB = war.length() / (1024.0 * 1024.0)
		logger.lifecycle("Uploading WAR file: ${war.name}")
		logger.lifecycle(String.format("File size: %.2f MB", fileSizeMB))
		logger.lifecycle("Target URL: ${uploadUrl()}")
	}

	/**
	 * Uploads the WAR file with retry. A 3xx or 4xx response is not retried. Anything else is tried
	 * up to MAX_RETRY_ATTEMPTS times, waiting 1s and then 2s between attempts.
	 */
	private fun uploadWarWithRetry(war: File, clientSocketFactory: SSLSocketFactory?) {
		var lastException: Exception? = null

		for (attempt in 1..MAX_RETRY_ATTEMPTS) {
			try {
				uploadWarFile(war, clientSocketFactory)
				return
			} catch (e: RejectedUploadException) {
				throw e
			} catch (e: Exception) {
				lastException = e
				if (attempt < MAX_RETRY_ATTEMPTS) {
					val delayMs = RETRY_DELAY_MS * attempt
					logger.warn("Upload attempt $attempt failed: ${e.message}. Retrying in ${delayMs}ms...")
					Thread.sleep(delayMs)
				}
			}
		}

		throw GradleException("Upload failed after $MAX_RETRY_ATTEMPTS attempts", lastException)
	}

	/**
	 * Uploads the WAR file using multipart/form-data.
	 */
	private fun uploadWarFile(war: File, clientSocketFactory: SSLSocketFactory?) {
		val boundary = "----WebKitFormBoundary" + System.currentTimeMillis()
		val appXml = resolveApplicationXml()

		val connection = createConnection(uploadUrl(), boundary, clientSocketFactory)
		try {
			writeMultipartBody(connection, boundary, appXml, war)

			val responseCode = connection.responseCode
			logger.lifecycle("Response: $responseCode - ${connection.responseMessage}")

			validateResponse(connection, responseCode)
			logResponseBody(connection)
		} finally {
			connection.disconnect()
		}
	}

	/**
	 * Writes the multipart/form-data body to the connection output stream.
	 * Sends two parts: "applicationXml" (text/xml) and "warFile" (application/octet-stream).
	 */
	private fun writeMultipartBody(connection: HttpURLConnection, boundary: String, appXml: String, war: File) {
		connection.outputStream.use { out ->

			// Part 1: applicationXml
			out.write("--$boundary\r\n".toByteArray(StandardCharsets.UTF_8))
			out.write("Content-Disposition: form-data; name=\"applicationXml\"\r\n".toByteArray(StandardCharsets.UTF_8))
			out.write("Content-Type: text/xml; charset=UTF-8\r\n".toByteArray(StandardCharsets.UTF_8))
			out.write("\r\n".toByteArray(StandardCharsets.UTF_8))
			out.write(appXml.toByteArray(StandardCharsets.UTF_8))
			out.write("\r\n".toByteArray(StandardCharsets.UTF_8))

			// Part 2: warFile
			out.write("--$boundary\r\n".toByteArray(StandardCharsets.UTF_8))
			out.write("Content-Disposition: form-data; name=\"warFile\"; filename=\"${war.name}\"\r\n".toByteArray(StandardCharsets.UTF_8))
			out.write("Content-Type: application/octet-stream\r\n".toByteArray(StandardCharsets.UTF_8))
			out.write("\r\n".toByteArray(StandardCharsets.UTF_8))

			FileInputStream(war).use { fileInput ->
				val buffer = ByteArray(BUFFER_SIZE)
				var totalBytes = 0L
				var lastLoggedMB = 0L

				while (true) {
					val bytesRead = fileInput.read(buffer)
					if (bytesRead == -1) {
						break
					}
					out.write(buffer, 0, bytesRead)
					totalBytes += bytesRead

					// Log progress every 100MB for large files
					val currentMB = totalBytes / (1024 * 1024)
					if (currentMB - lastLoggedMB >= 100) {
						logger.lifecycle("Uploaded: ${currentMB}MB")
						lastLoggedMB = currentMB
					}
				}

				logger.lifecycle(String.format("Total uploaded: %.2f MB", totalBytes / (1024.0 * 1024.0)))
			}

			// Final boundary
			out.write("\r\n".toByteArray(StandardCharsets.UTF_8))
			out.write("--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8))
		}
	}

	/**
	 * Validates the HTTP response code.
	 */
	private fun validateResponse(connection: HttpURLConnection, responseCode: Int) {
		if (responseCode != 200 && responseCode != 201) {
			val message = "Upload failed with code $responseCode: ${getErrorMessage(connection)}"
			// 3xx (redirects are never followed) and 4xx (refused) cannot succeed on retry, and
			// retrying bad credentials could lock the user ID.
			if (responseCode in 300..499) {
				throw RejectedUploadException(message)
			}
			throw GradleException(message)
		}
	}

	/**
	 * Returns the upload endpoint URL: the configured serverUrl plus /appdeploy/uploadApp.
	 * A serverUrl that already ends with the endpoint path is used as it is.
	 */
	private fun uploadUrl(): String {
		val base = cicsAppDeploy.serverUrl.trim().trimEnd('/')
		return if (base.endsWith(UPLOAD_PATH)) base else base + UPLOAD_PATH
	}

	/**
	 * Creates the HTTP connection configured for multipart/form-data upload with authentication.
	 */
	private fun createConnection(url: String, boundary: String, clientSocketFactory: SSLSocketFactory?): HttpURLConnection {
		val connection = URL(url).openConnection() as HttpURLConnection
		if (clientSocketFactory != null && connection is HttpsURLConnection) {
			connection.sslSocketFactory = clientSocketFactory
		}
		connection.requestMethod = "POST"
		connection.doOutput = true
		connection.instanceFollowRedirects = false  // Never follow redirects: a redirected POST would resend credentials and the WAR

		val connectTimeout = cicsAppDeploy.connectTimeout
		val readTimeout = cicsAppDeploy.readTimeout
		connection.connectTimeout = connectTimeout  // Time to establish connection
		connection.readTimeout = readTimeout        // Time to wait for response

		logger.lifecycle("Timeout configuration - Connect: ${connectTimeout}ms, Read: ${readTimeout}ms")

		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

		addAuthentication(connection, clientSocketFactory)

		return connection
	}

	/**
	 * Adds the authentication header to the connection (Bearer token or Basic Auth).
	 * A client certificate is sent in the TLS handshake, so it needs no header.
	 * If no credentials are provided, no Authorization header is added.
	 */
	private fun addAuthentication(connection: HttpURLConnection, clientSocketFactory: SSLSocketFactory?) {
		val bearerToken = cicsAppDeploy.bearerToken
		val userName = cicsAppDeploy.userName
		val password = cicsAppDeploy.password // pragma: allowlist secret

		if (bearerToken.isNotEmpty()) {
			connection.setRequestProperty("Authorization", "Bearer $bearerToken")
			logger.lifecycle("Using JWT Bearer token authentication")
		} else if (userName.isNotEmpty() && password.isNotEmpty()) {
			val encodedCredentials = Base64.getEncoder().encodeToString("$userName:$password".toByteArray())
			connection.setRequestProperty("Authorization", "Basic $encodedCredentials")
			logger.lifecycle("Using Basic Authentication")
		} else if (clientSocketFactory != null) {
			logger.lifecycle("Using client certificate authentication")
		} else {
			logger.lifecycle("No authentication - sending request without Authorization header")
		}
	}

	/**
	 * Logs the HTTP response body if available.
	 */
	private fun logResponseBody(connection: HttpURLConnection) {
		try {
			val responseBody = connection.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
			if (responseBody.isNotEmpty()) {
				logger.lifecycle("Server response: $responseBody")
			}
		} catch (e: Exception) {
			// Ignore
		}
	}

	/**
	 * Extracts the error message from the HTTP connection.
	 */
	private fun getErrorMessage(connection: HttpURLConnection): String {
		try {
			val errorStream = connection.errorStream
			if (errorStream != null) {
				val body = errorStream.bufferedReader().use { it.readText() }
				return if (body.isNotEmpty()) body else connection.responseMessage
			}
		} catch (e: Exception) {
			// Ignore
		}
		return try {
			connection.responseMessage
		} catch (e: Exception) {
			"Unknown error"
		}
	}

	/**
	 * Validates that all required configuration properties are set.
	 */
	private fun validateConfiguration() {
		val config = cicsAppDeploy
		val errors = StringBuilder()

		if (config.serverUrl.isEmpty()) {
			errors.append(MISSING_SERVER_URL).append("\n")
		}
		val hasInlineApplicationXml = config.applicationXml.trim().isNotEmpty()
		val hasApplicationXmlLocation = config.applicationXmlLocation.trim().isNotEmpty()

		if (!hasInlineApplicationXml && !hasApplicationXmlLocation) {
			errors.append(MISSING_APPLICATION_XML).append("\n")
		}

		if (hasInlineApplicationXml && hasApplicationXmlLocation) {
			logger.warn("Both applicationXml and applicationXmlLocation provided. Inline applicationXml will be used.")
		}

		val hasBasicAuth = config.userName.isNotEmpty() && config.password.isNotEmpty()
		val hasBearerToken = config.bearerToken.isNotEmpty()
		val hasClientCert = config.clientKeyStore.isNotEmpty()
		val hasPartialBasicAuth = (config.userName.isNotEmpty() && config.password.isEmpty()) ||
			(config.userName.isEmpty() && config.password.isNotEmpty())

		// Only error on partial Basic Auth if there's no bearerToken or clientKeyStore
		if (hasPartialBasicAuth && !hasBearerToken && !hasClientCert) {
			errors.append("Incomplete Basic Authentication. Provide both userName and password, or use bearerToken or clientKeyStore, or omit all credentials if the server uses auth=\"none\".").append("\n") // pragma: allowlist secret
		}

		if (hasClientCert) {
			val keyStoreFile = clientKeyStoreFile()
			if (!keyStoreFile.isFile) {
				errors.append("clientKeyStore does not exist: ").append(keyStoreFile.absolutePath).append("\n")
			}
		} else if (config.clientKeyStorePassword.isNotEmpty()) {
			errors.append("clientKeyStorePassword is set without clientKeyStore").append("\n")
		}

		if (!hasBasicAuth && !hasPartialBasicAuth && !hasBearerToken && !hasClientCert) {
			logger.warn("No authentication credentials provided. Request will be sent without authentication. The server must be configured with auth=\"none\".") // pragma: allowlist secret
		}

		if (errors.isNotEmpty()) {
			throw GradleException(errors.toString() + "\n" + UPLOAD_CONFIG_EXCEPTION)
		}
	}

	/**
	 * Resolves a configured path. A relative path is resolved against the project directory.
	 */
	private fun resolveFile(path: String): File {
		val file = File(path)
		return if (file.isAbsolute) file else File(projectDir, path)
	}

	private fun clientKeyStoreFile(): File = resolveFile(cicsAppDeploy.clientKeyStore)

	/**
	 * Loads the client keystore and returns a socket factory that presents its certificate, or null if no
	 * clientKeyStore is configured. The server certificate is checked against the JVM default truststore.
	 */
	private fun buildClientSocketFactory(): SSLSocketFactory? {
		if (cicsAppDeploy.clientKeyStore.isEmpty()) {
			return null
		}
		val file = clientKeyStoreFile()
		val password = cicsAppDeploy.clientKeyStorePassword.toCharArray()

		try {
			val keyStore = FileInputStream(file).use { input ->
				KeyStore.getInstance(cicsAppDeploy.clientKeyStoreType).also { it.load(input, password) }
			}

			if (!keyStore.aliases().asSequence().any { keyStore.isKeyEntry(it) }) {
				throw GradleException("clientKeyStore has no private key entry: ${file.absolutePath}")
			}

			val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
			keyManagers.init(keyStore, password)
			val context = SSLContext.getInstance("TLS")
			context.init(keyManagers.keyManagers, null, null)
			return context.socketFactory
		} catch (e: GradleException) {
			throw e
		} catch (e: Exception) {
			throw GradleException("Cannot load clientKeyStore ${file.absolutePath}: ${e.message}", e)
		}
	}

	private fun resolveApplicationXml(): String {
		val inlineApplicationXml = cicsAppDeploy.applicationXml
		if (inlineApplicationXml.trim().isNotEmpty()) {
			return inlineApplicationXml.trim()
		}

		val applicationXmlFile = resolveFile(cicsAppDeploy.applicationXmlLocation)

		if (!applicationXmlFile.exists()) {
			throw GradleException("applicationXmlLocation does not exist: ${applicationXmlFile.absolutePath}")
		}

		if (!applicationXmlFile.isFile) {
			throw GradleException("applicationXmlLocation is not a file: ${applicationXmlFile.absolutePath}")
		}

		try {
			return String(applicationXmlFile.readBytes(), StandardCharsets.UTF_8).trim()
		} catch (e: IOException) {
			throw GradleException("Failed to read applicationXmlLocation: ${applicationXmlFile.absolutePath}", e)
		}
	}
}
