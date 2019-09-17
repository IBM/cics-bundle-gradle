package com.ibm.cics.cbgp

/*-
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

import com.ibm.cics.bundle.parts.BundlePublisher
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.util.VersionNumber

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

abstract class AbstractBundleTask extends DefaultTask {


//
//	public static final String BAD_VERSION_NUMBER = 'Unable to parse project version number'
//	String defaultjvmserver

	protected BundlePublisher initBundlePublisher() throws GradleException {
//		println("OutputDirectory is: ${outputDirectory.toString()}")
//
//		VersionNumber versionNumber = getProjectVersionNumber()
//		File f = outputDirectory.getAsFile().get()
//
//
//		BundlePublisher bundlePublisher = new BundlePublisher(
//				f.toPath(),
//				project.getArtifactId(),
//				versionNumber.getMajor(),
//				versionNumber.getMinor(),
//				versionNumber.getPatch(),
//				versionNumber.getQualifier()
//		)
//		addStaticBundleResources(bundlePublisher)
//		return bundlePublisher
	}

	private void addStaticBundleResources(BundlePublisher bundlePublisher) throws GradleException {
		//Add bundle parts for any resources
//		Path basePath = baseDir.toPath()
//		Path bundlePartSource = basePath.resolve("src/main/resources")
//		getLog().info("Gathering bundle parts from " + basePath.relativize(bundlePartSource))
//
//		if (Files.exists(bundlePartSource)) {
//			if (Files.isDirectory(bundlePartSource)) {
//				try {
//					List<Path> paths = Files
//							.walk(bundlePartSource)
//							.filter(Files::isRegularFile)
//							.collect(Collectors.toList())
//
//					for (Path toAdd : paths) {
//						try {
//							bundlePublisher.addStaticResource(
//									bundlePartSource.relativize(toAdd),
//									() -> Files.newInputStream(toAdd)
//							)
//						} catch (BundlePublisher.PublishException e) {
//							throw new GradleException("Failure adding static resource " + toAdd + ": " + e.getMessage(), e)
//						}
//					}
//				} catch (IOException e) {
//					throw new GradleException("Failure adding static resources", e)
//				}
//			} else {
//				throw new GradleException("Static bundle resources directory " + bundlePartSource + "wasn't a directory")
//			}
//		} else {
//			//Ignore if it doesn't exist
//		}
	}


//	private VersionNumber getProjectVersionNumber() throws GradleException {
//		def pv = project.version
//		if (!pv instanceof String) {
//			throw new GradleException(BAD_VERSION_NUMBER)
//		} else {
//			VersionNumber versionNumber = VersionNumber.parse((String) pv)
//			if (versionNumber == VersionNumber.UNKNOWN) {
//				throw new GradleException(BAD_VERSION_NUMBER)
//			}
//			return versionNumber
//		}
//	}

}
