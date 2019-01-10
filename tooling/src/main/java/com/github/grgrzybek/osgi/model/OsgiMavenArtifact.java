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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.framework.Version;

/**
 * OSGi Maven Artifact is downloadable from Maven Central and includes packages at different versions.
 * Single package in single artifact may have only one version (I guess).
 */
public class OsgiMavenArtifact implements Comparable<OsgiMavenArtifact> {

    private String groupId;
    private String artifactId;
    private String version;
    private Version _v;

    private Map<String, OsgiPackage> foundPackages = new LinkedHashMap<>();

    public OsgiMavenArtifact(String mvnURI) {
        String groupId = null;
        String artifactId = null;
        String version = null;
        String type = null;

        String[] segments = mvnURI.split("/");
        if (segments.length == 3) {
            groupId = segments[0];
            artifactId = segments[1];
            version = segments[2];
            type = "jar";
        } else if (segments.length == 4) {
            groupId = segments[0];
            artifactId = segments[1];
            type = segments[3];
            version = segments[3];
        }

        if (version != null) {
            _v = new Version(version);
        }

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public OsgiMavenArtifact(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OsgiMavenArtifact that = (OsgiMavenArtifact) o;
        return groupId.equals(that.groupId) &&
                artifactId.equals(that.artifactId) &&
                version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getType() {
        String type = "jar";
        return type;
    }

    public List<OsgiPackage> getFoundPackages() {
        return new LinkedList<>(foundPackages.values());
    }

    public void addPackage(String pkg, String version, OsgiPackage.From from) {
        if (!foundPackages.containsKey(pkg) || from == OsgiPackage.From.ANNOTATION) {
            // annotation-specified version has priority over packageinfo file
            foundPackages.put(pkg, new OsgiPackage(new PackageName(pkg), version));
        }
    }

    @Override
    public int compareTo(OsgiMavenArtifact o) {
        return this._v.compareTo(o._v);
    }

    public String toMvnURI() {
        return String.format("%s/%s/%s", groupId, artifactId, version);
    }

    public String toLink() {
        return String.format("http://repo.maven.apache.org/maven2/%s/%s/%s", groupId.replaceAll("\\.", "/"), artifactId, version);
    }

}
