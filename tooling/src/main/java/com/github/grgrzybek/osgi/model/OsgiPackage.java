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

/**
 * An OSGi package that is uniquely identified by name and may have different versions available in different
 * {@link OsgiMavenArtifact artifacts}. Single package from one {@link OsgiMavenArtifact} has single version.
 */
public class OsgiPackage {

    private final PackageName packageName;
    private String version;

    public OsgiPackage(PackageName pkg, String version) {
        this.packageName = pkg;
        this.version = version;
    }

    public PackageName getPackageName() {
        return packageName;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "OsgiPackage{" + "packageName=" + packageName + ", version='" + version + '\'' + '}';
    }

    public enum From {
        /** Package version was found in package-info.class */
        ANNOTATION,
        /** Package version was found in packageinfo file (old, pre JDK5 bundle) */
        PACKAGEINFO
    }

}
