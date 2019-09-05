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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskProvider

import javax.inject.Inject
import java.util.concurrent.Callable

// Extension block for build config
class BuildExtension {
    String defaultjvmserver = ''
}

// Extension block for deploy config
class DeployExtension {
    String cicsplex = ''
    String region = ''
    String bunddef = ''
    String csdgroup = ''
    String url = ''
    String username = ''
    String password = ''
}

class BundlePlugin implements Plugin<Project> {

    public static final String BUILD_TASK_NAME = 'buildCICSBundle'
    public static final String ARCHIVE_TASK_NAME = 'archiveCICSBundle'
    public static final String DEPLOY_TASK_NAME = 'deployCICSBundle'
    public static final String BUILD_EXTENSION_NAME = BUILD_TASK_NAME + 'Config'
    public static final String DEPLOY_EXTENSION_NAME = DEPLOY_TASK_NAME + 'Config'

    public static final String BUNDLE_DEPENDENCY_CONFIGURATION_NAME = "cicsBundle"

    private final ObjectFactory objectFactory;

    @Inject
    public BundlePlugin(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    void apply(Project project) {
        project.getPluginManager().apply(BasePlugin.class);
        project.extensions.create(BUILD_EXTENSION_NAME, BuildExtension)
        project.extensions.create(DEPLOY_EXTENSION_NAME, DeployExtension)

        project.getTasks().withType(BuildBundleTask).configureEach {
            it.dependsOn {
                project.getConfigurations().getByName(BUNDLE_DEPENDENCY_CONFIGURATION_NAME)
            }
        }

        TaskProvider<BuildBundleTask> build = project.tasks.register(BUILD_TASK_NAME, BuildBundleTask, {
            it.setDescription("Generates a CICS bundle with all the bundle parts including external dependencies.")
            it.setGroup(BasePlugin.BUILD_GROUP)
        })

        project.tasks.register(DEPLOY_TASK_NAME, DeployBundleTask, {
            it.setDescription("Deploys a CICS bundle to a CICS system.")
            it.setGroup(BasePlugin.UPLOAD_GROUP)
        })

        configureConfigurations(project.getConfigurations())
    }

    private void configureConfigurations(ConfigurationContainer configurationContainer) {
        configurationContainer.create(BUNDLE_DEPENDENCY_CONFIGURATION_NAME).setVisible(false).
                setDescription("Dependencies that constitute bundle parts that should be included in this CICS bundle.");
    }
}
