/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.lavender.modules;

import net.oneandone.lavender.config.Filter;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarModule extends Module {
    /** To properly make jars available as a module, I have to load them into memory when the jar is itself packaged into a war. */
    public static Object[] fromJar(Filter filter, String type, JarModuleConfig config, Node jar) throws IOException {
        World world;
        ZipEntry entry;
        String path;
        ZipInputStream src;
        Node root;
        Node child;
        boolean isProperty;
        Node propertyNode;
        Map<String, Node> files;
        String resourcePath;

        world = jar.getWorld();
        root = world.getMemoryFilesystem().root().node(UUID.randomUUID().toString(), null).mkdir();
        src = new ZipInputStream(jar.createInputStream());
        propertyNode = null;
        files = new HashMap<>();
        resourcePath = null; // definite assignment
        while ((entry = src.getNextEntry()) != null) {
            path = entry.getName();
            if (!entry.isDirectory()) {
                isProperty = WarModule.PROPERTIES.equals(path);
                if (isProperty || ((resourcePath = config.getPath(path)) != null && filter.isIncluded(path))) {
                    child = root.join(path);
                    child.getParent().mkdirsOpt();
                    world.getBuffer().copy(src, child);
                    if (isProperty) {
                        propertyNode = child;
                    } else {
                        files.put(resourcePath, child);
                    }
                }
            }
        }
        return new Object[] { new JarModule(type, config.getModuleName(), files), propertyNode };
    }

    private final Map<String, Node> files;

    public JarModule(String type, String moduleName, Map<String, Node> files) throws IOException {
        super(type, moduleName, true, "");
        this.files = files;
    }

    public Iterator<Resource> iterator() {
        return new JarResourceIterator(files.entrySet().iterator());
    }

    public Resource probe(String path) throws IOException {
        Node file;

        file = files.get(path);
        return file == null ? null : DefaultResource.forNode(file, path);
    }

    @Override
    public void saveCaches() {
        // nothing to do
    }
}
