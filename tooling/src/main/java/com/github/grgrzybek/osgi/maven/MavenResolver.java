/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.grgrzybek.osgi.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.grgrzybek.osgi.model.OsgiMavenArtifact;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.slf4j.Slf4jLoggerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.wagon.WagonProvider;
import org.eclipse.aether.transport.wagon.WagonTransporterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenResolver {

    public static Logger LOG = LoggerFactory.getLogger(MavenResolver.class);

    private RepositorySystem repository;
    private RemoteRepository central;

    public MavenResolver() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.setService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, WagonTransporterFactory.class);
        locator.addService(WagonProvider.class, HttpWagonProvider.class);
        locator.setService(org.eclipse.aether.spi.log.LoggerFactory.class, Slf4jLoggerFactory.class);
        repository = locator.getService(RepositorySystem.class);

        RemoteRepository.Builder builder = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2");
        RepositoryPolicy enabledPolicy = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        RepositoryPolicy disabledPolicy = new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        builder.setReleasePolicy(enabledPolicy);
        builder.setSnapshotPolicy(disabledPolicy);
        central = builder.build();
    }

    /**
     * Resolves maven URIs in the form of {@link OsgiMavenArtifact}
     * @param artifact
     * @return
     */
    public File resolve(OsgiMavenArtifact artifact) throws IOException {

        ArtifactRequest req = new ArtifactRequest();
        req.setArtifact(new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getType(), artifact.getVersion()));
        req.addRepository(central);

        LOG.trace("Request: {}", req);

        try {
            ArtifactResult res = repository.resolveArtifact(session(), req);
            LOG.trace("Result: {}", res);

            File file = res.getArtifact().getFile();
            LOG.trace("Result file: {}", file);

            return file;
        } catch (ArtifactResolutionException e) {
            // we know there's one ArtifactResult, because there was one ArtifactRequest
            ArtifactResolutionException original = new ArtifactResolutionException(e.getResults(),
                    "Error resolving artifact " + req.getArtifact(), null);
            original.setStackTrace(e.getStackTrace());

            List<String> messages = new ArrayList<>(e.getResult().getExceptions().size());
            List<Exception> suppressed = new ArrayList<>();
            for (Exception ex : e.getResult().getExceptions()) {
                messages.add(ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage());
                suppressed.add(ex);
            }
            IOException exception = new IOException(original.getMessage() + ": " + messages, original);
            for (Exception ex : suppressed) {
                exception.addSuppressed(ex);
            }
            LOG.warn(exception.getMessage(), exception);

            throw exception;
        }
    }

    private RepositorySystemSession session() {
        RepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        ((DefaultRepositorySystemSession)session).setConfigProperty("aether.connector.basic.threads", "1");

        String basedir = "target/local.repository";
        new File(basedir).mkdirs();
        LocalRepositoryManager localRepositoryManager
                = repository.newLocalRepositoryManager(session, new LocalRepository(basedir));
        ((DefaultRepositorySystemSession)session).setLocalRepositoryManager(localRepositoryManager);

        return session;
    }

}
