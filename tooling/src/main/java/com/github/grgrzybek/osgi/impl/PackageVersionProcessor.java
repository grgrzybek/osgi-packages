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
package com.github.grgrzybek.osgi.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.github.grgrzybek.osgi.ArtifactProcessor;
import com.github.grgrzybek.osgi.model.OsgiMavenArtifact;
import com.github.grgrzybek.osgi.model.OsgiPackage;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageVersionProcessor implements ArtifactProcessor {

    public static Logger LOG = LoggerFactory.getLogger(PackageVersionProcessor.class);

    private OsgiMavenArtifact processedArtifact;

    public PackageVersionProcessor(OsgiMavenArtifact processedArtifact) {
        this.processedArtifact = processedArtifact;
    }

    @Override
    public void processDirectory(String pkg, ZipArchiveEntry entry) {
    }

    @Override
    public void processResource(String pkg, ZipFile jar, ZipArchiveEntry entry) {
        File f = new File(entry.getName());
        try {
            if (f.getName().equals("package-info.class")) {
                try (InputStream entryStream = jar.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(entryStream);
                    reader.accept(new ClassVisitor(Opcodes.ASM6) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                            return new AnnotationVisitor(Opcodes.ASM6) {
                                @Override
                                public void visit(String name, Object version) {
                                    SignatureReader sr = new SignatureReader(descriptor);
                                    sr.accept(new SignatureVisitor(Opcodes.ASM6) {
                                        @Override
                                        public void visitClassType(String name) {
                                            if ("org/osgi/annotation/versioning/Version".equals(name)) {
                                                LOG.trace("Found package {}; version {}", pkg, version);
                                                processedArtifact.addPackage(pkg, Objects.toString(version), OsgiPackage.From.ANNOTATION);
                                            }
                                        }
                                    });
                                }
                            };
                        }
                    }, 0);
                }
            } else if (f.getName().equals("packageinfo")) {
                try (InputStream entryStream = jar.getInputStream(entry)) {
                    StringWriter sw = new StringWriter();
                    IOUtils.copy(entryStream, sw, StandardCharsets.UTF_8);
                    String[] v = sw.toString().split(" ");
                    String version = "?";
                    for (int i = 0; i < v.length - 1; i++) {
                        if (v[i].equals("version")) {
                            version = v[i + 1].trim();
                            break;
                        }
                    }
                    LOG.trace("Found package {}; version {}", pkg, version);
                    processedArtifact.addPackage(pkg, version, OsgiPackage.From.PACKAGEINFO);
                }
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

}
