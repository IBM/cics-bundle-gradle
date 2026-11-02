/*
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

import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

/**
 * Tests for the com.ibm.cics.appdeploy plugin and its uploadWarToLiberty task.
 */
class AppDeployTests extends Specification {

	static final String TASK = AppDeployPlugin.UPLOAD_TASK_NAME
	static final String UPLOAD_PATH = "/appdeploy/uploadApp"
	static final String APP_XML = '<application id="myapp" location="appdeploy-1.0.0.war" type="war"/>'

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort())

	File projectDir

	def setup() {
		ExpandoMetaClass.disableGlobally()
		File src = new File("build/resources/test/appdeploy")
		projectDir = new File("build/test-projects/${this.class.simpleName}/${specificationContext.currentIteration.name.replaceAll('[^A-Za-z0-9._-]', '_')}")
		FileUtils.deleteDirectory(projectDir)
		FileUtils.copyDirectory(src, projectDir)
	}

	private String serverUrl() {
		return "http://localhost:${wireMockRule.port()}"
	}

	private BuildResult run(boolean failExpected, String gradleVersion, Map<String, String> props) {
		List<String> args = [TASK, "--stacktrace"]
		props.each { k, v -> args << "-P${k}=${v}".toString() }
		GradleRunner runner = GradleRunner.create()
				.withProjectDir(projectDir)
				.withArguments(args)
				.withPluginClasspath()
				.withGradleVersion(gradleVersion)
		BuildResult result = failExpected ? runner.buildAndFail() : runner.build()
		println(result.output)
		return result
	}

	private static void outputContains(BuildResult result, List<String> expected) {
		expected.each { assert result.output.contains(it): "Missing output string '$it'" }
	}

	private void stubUpload(int status, String body = "OK") {
		stubFor(post(urlEqualTo(UPLOAD_PATH)).willReturn(aResponse().withStatus(status).withBody(body)))
	}

	@Unroll
	def "Upload with Basic authentication on Gradle #gradleVersion"(String gradleVersion) {
		given:
		stubUpload(200, "Application deployed")

		when:
		def result = run(false, gradleVersion, [uploadUrl: serverUrl(), uploadAppXml: APP_XML, uploadUser: "someuser", uploadPassword: "somepassword"]) // pragma: allowlist secret

		then:
		outputContains(result, ["Using Basic Authentication", "Uploading WAR file: appdeploy-1.0.0.war", "Server response: Application deployed", "WAR file uploaded successfully!"])
		verify(1, postRequestedFor(urlEqualTo(UPLOAD_PATH))
				.withHeader("Authorization", equalTo("Basic " + "someuser:somepassword".bytes.encodeBase64().toString()))
				.withHeader("Content-Type", containing("multipart/form-data"))
				.withRequestBody(containing('name="applicationXml"'))
				.withRequestBody(containing(APP_XML))
				.withRequestBody(containing('name="warFile"; filename="appdeploy-1.0.0.war"')))

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}

	@Unroll
	def "Upload with bearer token on Gradle #gradleVersion"(String gradleVersion) {
		given:
		stubUpload(200)

		when:
		def result = run(false, gradleVersion, [uploadUrl: serverUrl(), uploadAppXml: APP_XML, uploadToken: "sometoken"])

		then:
		outputContains(result, ["Using JWT Bearer token authentication", "WAR file uploaded successfully!"])
		verify(1, postRequestedFor(urlEqualTo(UPLOAD_PATH)).withHeader("Authorization", equalTo("Bearer sometoken")))

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}

	@Unroll
	def "Upload with no authentication on Gradle #gradleVersion"(String gradleVersion) {
		given:
		stubUpload(200)

		when:
		def result = run(false, gradleVersion, [uploadUrl: serverUrl(), uploadAppXml: APP_XML])

		then:
		outputContains(result, ["No authentication - sending request without Authorization header", "WAR file uploaded successfully!"])
		verify(1, postRequestedFor(urlEqualTo(UPLOAD_PATH)).withoutHeader("Authorization"))

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}

	@Unroll
	def "Application XML is read from applicationXmlLocation on Gradle #gradleVersion"(String gradleVersion) {
		given:
		stubUpload(200)
		new File(projectDir, "application.xml").text = APP_XML

		when:
		def result = run(false, gradleVersion, [uploadUrl: serverUrl() + "/", uploadAppXmlLocation: "application.xml"])

		then:
		outputContains(result, ["WAR file uploaded successfully!"])
		verify(1, postRequestedFor(urlEqualTo(UPLOAD_PATH)).withRequestBody(containing(APP_XML)))

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}

	@Unroll
	def "Upload task builds the WAR first on Gradle #gradleVersion"(String gradleVersion) {
		given:
		stubUpload(200)

		when:
		def result = run(false, gradleVersion, [uploadUrl: serverUrl(), uploadAppXml: APP_XML])

		then:
		new File(projectDir, "build/libs/appdeploy-1.0.0.war").isFile()
		outputContains(result, ["Task :war", "Task :$TASK"])

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}

	@Unroll
	def "Upload task runs again when nothing has changed on Gradle #gradleVersion"(String gradleVersion) {
		given:
		stubUpload(200)
		def props = [uploadUrl: serverUrl(), uploadAppXml: APP_XML]

		when:
		def first = run(false, gradleVersion, props)
		def second = run(false, gradleVersion, props)

		then:
		first.task(":$TASK").outcome == TaskOutcome.SUCCESS
		second.task(":$TASK").outcome == TaskOutcome.SUCCESS
		// The WAR is unchanged, so the war task may be up to date, but the upload must not be
		second.task(":war").outcome == TaskOutcome.UP_TO_DATE
		verify(2, postRequestedFor(urlEqualTo(UPLOAD_PATH)))

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}

	@Unroll
	def "Missing serverUrl fails on Gradle #gradleVersion"(String gradleVersion) {
		when:
		def result = run(true, gradleVersion, [uploadAppXml: APP_XML])

		then:
		outputContains(result, [UploadWarToLibertyTask.MISSING_SERVER_URL])

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}

	@Unroll
	def "Missing application XML fails on Gradle #gradleVersion"(String gradleVersion) {
		when:
		def result = run(true, gradleVersion, [uploadUrl: serverUrl()])

		then:
		outputContains(result, [UploadWarToLibertyTask.MISSING_APPLICATION_XML])

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}

	@Unroll
	def "Partial Basic authentication fails on Gradle #gradleVersion"(String gradleVersion) {
		when:
		def result = run(true, gradleVersion, [uploadUrl: serverUrl(), uploadAppXml: APP_XML, uploadUser: "someuser"])

		then:
		outputContains(result, ["Incomplete Basic Authentication"])
		verify(0, postRequestedFor(urlEqualTo(UPLOAD_PATH)))

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}

	@Unroll
	def "A file that is not a WAR is rejected on Gradle #gradleVersion"(String gradleVersion) {
		given:
		new File(projectDir, "notawar.txt").text = "not a war"
		new File(projectDir, "build.gradle") << "\ntasks.named('${TASK}') { warFile = file('notawar.txt') }\n"

		when:
		def result = run(true, gradleVersion, [uploadUrl: serverUrl(), uploadAppXml: APP_XML])

		then:
		outputContains(result, ["Unsupported file type: .txt. Only .war files are accepted"])
		verify(0, postRequestedFor(urlEqualTo(UPLOAD_PATH)))

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}

	@Unroll
	def "A custom warFile is uploaded on Gradle #gradleVersion"(String gradleVersion) {
		given:
		stubUpload(200)
		new File(projectDir, "other.war").text = "pretend war"
		new File(projectDir, "build.gradle") << "\ntasks.named('${TASK}') { warFile = file('other.war') }\n"

		when:
		def result = run(false, gradleVersion, [uploadUrl: serverUrl(), uploadAppXml: APP_XML])

		then:
		outputContains(result, ["Uploading WAR file: other.war"])
		verify(1, postRequestedFor(urlEqualTo(UPLOAD_PATH)).withRequestBody(containing('filename="other.war"')))

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}

	@Unroll
	def "A #status response is not retried on Gradle #gradleVersion"(int status, String gradleVersion) {
		given:
		stubUpload(status, "refused")

		when:
		def result = run(true, gradleVersion, [uploadUrl: serverUrl(), uploadAppXml: APP_XML])

		then:
		outputContains(result, ["Upload failed with code $status: ".toString()])
		verify(1, postRequestedFor(urlEqualTo(UPLOAD_PATH)))

		where:
		[status, gradleVersion] << [[302, 403], GradleVersions.GRADLE_VERSIONS].combinations()
	}

	@Unroll
	def "A 503 response is retried on Gradle #gradleVersion"(String gradleVersion) {
		given:
		stubUpload(503, "unavailable")

		when:
		def result = run(true, gradleVersion, [uploadUrl: serverUrl(), uploadAppXml: APP_XML])

		then:
		outputContains(result, ["Upload failed after 3 attempts"])
		verify(3, postRequestedFor(urlEqualTo(UPLOAD_PATH)))

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}

	@Unroll
	def "clientKeyStore that does not exist fails on Gradle #gradleVersion"(String gradleVersion) {
		when:
		def result = run(true, gradleVersion, [uploadUrl: serverUrl(), uploadAppXml: APP_XML, uploadKeyStore: "missing.p12"])

		then:
		outputContains(result, ["clientKeyStore does not exist: "])
		verify(0, postRequestedFor(urlEqualTo(UPLOAD_PATH)))

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}

	@Unroll
	def "clientKeyStorePassword without clientKeyStore fails on Gradle #gradleVersion"(String gradleVersion) {
		when:
		def result = run(true, gradleVersion, [uploadUrl: serverUrl(), uploadAppXml: APP_XML, uploadKeyStorePassword: "somepassword"]) // pragma: allowlist secret

		then:
		outputContains(result, ["clientKeyStorePassword is set without clientKeyStore"])

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}

	@Unroll
	def "Unreachable host fails after retries on Gradle #gradleVersion"(String gradleVersion) {
		when:
		def result = run(true, gradleVersion, [uploadUrl: "http://localhost:1", uploadAppXml: APP_XML])

		then:
		outputContains(result, ["Failed to upload WAR file: Upload failed after 3 attempts"])

		where:
		gradleVersion << GradleVersions.GRADLE_VERSIONS
	}
}
