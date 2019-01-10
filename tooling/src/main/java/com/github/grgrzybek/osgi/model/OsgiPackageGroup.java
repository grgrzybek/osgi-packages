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
package com.github.grgrzybek.osgi.model;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For given package name, this class holds information about versions and the list of {@link OsgiMavenArtifact}
 * it is found in.
 */
public class OsgiPackageGroup {

    public static Logger LOG = LoggerFactory.getLogger(OsgiPackageGroup.class);

    private final PackageName packageName;
    private Map<Version, Set<OsgiMavenArtifact>> versionMapping = new TreeMap<>();

    public OsgiPackageGroup(PackageName pkg) {
        LOG.info("→ computing for " + pkg.toString());
        this.packageName = pkg;
    }

    public PackageName getPackageName() {
        return packageName;
    }

    public void addPackage(String pkg, String version, OsgiMavenArtifact from) {
        Version v = new Version(version);
        versionMapping.computeIfAbsent(v, (_v) -> new TreeSet<>()).add(from);
    }

    public Map<Version, Set<OsgiMavenArtifact>> getVersionMapping() {
        return versionMapping;
    }

}
