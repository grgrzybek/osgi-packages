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
package com.github.grgrzybek.osgi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.github.grgrzybek.osgi.impl.PackageVersionProcessor;
import com.github.grgrzybek.osgi.maven.MavenResolver;
import com.github.grgrzybek.osgi.model.OsgiMavenArtifact;
import com.github.grgrzybek.osgi.model.OsgiPackage;
import com.github.grgrzybek.osgi.model.OsgiPackageGroup;
import com.github.grgrzybek.osgi.model.PackageName;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JarStructureTest {

    public static Logger LOG = LoggerFactory.getLogger(JarStructureTest.class);

    @Test
    public void osgiCore() throws IOException {
        process("../metadata/osgi.core.jars.txt", "target/osgi.core.packages.xml");
    }

    @Test
    public void osgiCmpn() throws IOException {
        process("../metadata/osgi.cmpn.jars.txt", "target/osgi.cmpn.packages.xml");
    }

    private void process(String metadata, String report) throws IOException {
        MavenResolver resolver = new MavenResolver();

        List<String> mvnURIs = new LinkedList<>();
        BufferedReader br = new BufferedReader(new FileReader(metadata));
        String line = null;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#") || "".equals(line)) {
                continue;
            }
            mvnURIs.add(line);
        }

        Map<PackageName, OsgiPackageGroup> osgi = new TreeMap<>();

        for (String mvnURI : mvnURIs) {
            LOG.info("Checking {}", mvnURI);

            OsgiMavenArtifact mvnArtifact = new OsgiMavenArtifact(mvnURI);
            File fileArtifact = resolver.resolve(mvnArtifact);

            ArtifactProcessor processor = new PackageVersionProcessor(mvnArtifact);

            ZipFile jar = new ZipFile(fileArtifact);
            for (Enumeration<ZipArchiveEntry> e = jar.getEntries(); e.hasMoreElements();) {
                ZipArchiveEntry entry = e.nextElement();
                if (entry.getName().startsWith("org")) {
                    if (entry.isDirectory()) {
                        processor.processDirectory(entry.getName().replaceAll("/", "."), entry);
                    } else if (!entry.isUnixSymlink()) {
                        String name = entry.getName().substring(0, entry.getName().lastIndexOf('/'));
                        processor.processResource(name.replaceAll("/", "."), jar, entry);
                    }
                }
            }
            jar.close();

            for (OsgiPackage osgiPackage : mvnArtifact.getFoundPackages()) {
                PackageName pn = osgiPackage.getPackageName();
                osgi.computeIfAbsent(pn, (_pn) -> new OsgiPackageGroup(pn))
                        .addPackage(pn.toString(), osgiPackage.getVersion(), mvnArtifact);
            }
        }

        try (FileWriter fw = new FileWriter(report)) {
            fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            fw.write("<osgi-packages>\n");
            for (PackageName name : osgi.keySet()) {
                OsgiPackageGroup group = osgi.get(name);

                fw.write("  <package name=\"" + name.toString() + "\">\n");
                for (Map.Entry<Version, Set<OsgiMavenArtifact>> e : group.getVersionMapping().entrySet()) {
                    fw.write("    <version id=\"" + e.getKey().toString() + "\">\n");
                    for (OsgiMavenArtifact artifact : e.getValue()) {
                        fw.write("      <mvn uri=\"mvn:" + artifact.toMvnURI() + "\" link=\"" + artifact.toLink() + "\" />\n");
                    }
                    fw.write("    </version>\n");
                }

                fw.write("  </package>\n");

                StringWriter sw = new StringWriter();
                sw.append(name.toString()).append(": ");
                String versions = group.getVersionMapping().entrySet().stream()
                        .map((e) -> e.getKey().toString() + " (" + e.getValue().size() + ")")
                        .collect(Collectors.joining(", "));
                sw.append(versions);
                LOG.info(sw.toString());
            }
            fw.write("</osgi-packages>\n");
        }
    }

}
