/*
 * #%L
 * CICS Bundle Gradle Plugin
 * %%
 * Copyright (C) 2019 IBM Corp.
 * %%
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * #L%
 */
package com.ibm.cics.cbgp

import static com.github.tomakehurst.wiremock.client.WireMock.*

class WireMock extends AbstractTest {

	static void setupWiremock() {
		stubFor(
			post(urlEqualTo("/managedcicsbundles"))
				.withMultipartRequestBody(aMultipart().withName("cicsplex").withBody(equalTo("CICSEX56")))
				.withMultipartRequestBody(aMultipart().withName("region").withBody(equalTo("IYCWEMW2")))
				.withMultipartRequestBody(aMultipart().withName("bunddef").withBody(matching("GRADLE.*")))
				.withMultipartRequestBody(aMultipart().withName("csdgroup").withBody(equalTo("GRADLE")))
				.withMultipartRequestBody(aMultipart().withName("bundle"))
				.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/plain").withBody("Some content"))
		)
	}
}
