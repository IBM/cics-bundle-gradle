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

/**
 * Configuration for uploading a WAR file to a Liberty JVM server. The names and defaults match the
 * cicsAppDeploy configuration of cics-appdeploy-maven-plugin.
 */
open class LibertyWarUploadExtension {
	/** Base URL of the Liberty server. The plugin adds /appdeploy/uploadApp itself. */
	var serverUrl = ""

	/** Inline Liberty application XML. If specified, this takes precedence over applicationXmlLocation. */
	var applicationXml = ""

	/** File location for Liberty application XML. A relative path is resolved against the project directory. */
	var applicationXmlLocation = ""

	/** Username for Basic Authentication. */
	var userName = ""

	/** Password for Basic Authentication. */
	var password = ""

	/** JWT Bearer token for authentication. */
	var bearerToken = ""

	/** Keystore with the client certificate and private key, for a server with auth set to client-cert. */
	var clientKeyStore = ""

	/** Password of the client keystore. It is also used for the private key. */
	var clientKeyStorePassword = ""

	/** Type of the client keystore. */
	var clientKeyStoreType = "PKCS12"

	/** Time to establish the connection, in milliseconds. */
	var connectTimeout = 30000

	/** Time to wait for the server's response, in milliseconds. */
	var readTimeout = 300000
}
