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

import java.util.Objects;

public class PackageName implements Comparable<PackageName> {

    private String pkg;
    private String[] pkgSegments;

    public PackageName(String pkg) {
        this.pkg = pkg;
        this.pkgSegments = pkg.split("\\.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PackageName that = (PackageName) o;
        return pkg.equals(that.pkg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pkg);
    }

    @Override
    public int compareTo(PackageName o) {
        int count = Math.min(pkgSegments.length, o.pkgSegments.length);
        for (int i = 0; i < count; i++) {
            int v = pkgSegments[i].compareTo(o.pkgSegments[i]);
            if (v != 0) {
                return v;
            }
        }
        return pkgSegments.length - o.pkgSegments.length;
    }

    @Override
    public String toString() {
        return pkg;
    }

}
