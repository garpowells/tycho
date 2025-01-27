/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.ds;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.maven.SessionScoped;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ResolvedArtifactKey;
import org.eclipse.tycho.classpath.ClasspathContributor;
import org.eclipse.tycho.core.DeclarativeServicesConfiguration;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.maven.MavenDependenciesResolver;
import org.eclipse.tycho.core.osgitools.AbstractSpecificationClasspathContributor;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

@Component(role = ClasspathContributor.class, hint = "ds-annotations")
@SessionScoped
public class DeclarativeServicesClasspathContributor extends AbstractSpecificationClasspathContributor
		implements ClasspathContributor {

	private static final String DS_ANNOTATIONS_PACKAGE = "org.osgi.service.component.annotations";

	private static final String DS_ANNOTATIONS_GROUP_ID = "org.osgi";
	private static final String DS_ANNOTATIONS_ARTIFACT_ID = "org.osgi.service.component.annotations";

	private static final String DS_ANNOTATIONS_1_2_ARTIFACT_ID = "osgi.cmpn";
	private static final String DS_ANNOTATIONS_1_2_VERSION = "5.0.0";

	@Requirement
	DeclarativeServiceConfigurationReader configurationReader;

	@Requirement
	MavenDependenciesResolver dependenciesResolver;

	@Requirement
	TychoProjectManager projectManager;

	@Inject
	public DeclarativeServicesClasspathContributor(MavenSession session) {
		super(session, DS_ANNOTATIONS_PACKAGE, DS_ANNOTATIONS_GROUP_ID, DS_ANNOTATIONS_ARTIFACT_ID);
	}

	@Override
	protected Optional<ResolvedArtifactKey> findBundle(MavenProject project, VersionRange specificationVersion) {
		return super.findBundle(project, specificationVersion).or(() -> {
			Version v = specificationVersion.getLeft();
			if (v.getMajor() == 1 && v.getMinor() == 2) {
				// this is another artifact see https://github.com/osgi/osgi/issues/570
				try {
					Artifact artifact = dependenciesResolver.resolveArtifact(project, session, DS_ANNOTATIONS_GROUP_ID,
							DS_ANNOTATIONS_1_2_ARTIFACT_ID, DS_ANNOTATIONS_1_2_VERSION);
					ArtifactKey artifactKey = projectManager.getArtifactKey(artifact);
					return Optional.of(ResolvedArtifactKey.of(artifactKey, artifact.getFile()));
				} catch (ArtifactResolutionException e) {
					// can't resolve it ... nothing more to do!
				}
			}
			return Optional.empty();
		});
	}

	@Override
	protected VersionRange getSpecificationVersion(MavenProject project) {
		try {
			DeclarativeServicesConfiguration configuration = configurationReader.getConfiguration(project);
			if (configuration != null) {
				Version lowerVersion = configuration.getSpecificationVersion();
				Version upperVersion = new Version(lowerVersion.getMajor(), lowerVersion.getMinor() + 1, 0);
				return new VersionRange(VersionRange.LEFT_CLOSED, lowerVersion, upperVersion, VersionRange.RIGHT_OPEN);
			}
		} catch (IOException e) {
			// can't determine the minimum specification version then...
		}
		return new VersionRange(VersionRange.LEFT_CLOSED,
				Version.parseVersion(DeclarativeServiceConfigurationReader.DEFAULT_DS_VERSION), null,
				VersionRange.RIGHT_OPEN);
	}


}
