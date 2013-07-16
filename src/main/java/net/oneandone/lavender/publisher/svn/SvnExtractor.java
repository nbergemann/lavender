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
package net.oneandone.lavender.publisher.svn;

import net.oneandone.lavender.publisher.Extractor;
import net.oneandone.lavender.publisher.Resource;
import net.oneandone.lavender.publisher.config.Filter;
import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/** Extracts resources from svn */
public class SvnExtractor extends Extractor {
    private final List<Node> resources;
    private final String name;
    private final Node dest;

    public SvnExtractor(Filter filter, String storage, boolean lavendelize, String pathPrefix,
                        List<Node> resources, String name, Node dest) {
        super(filter, storage, lavendelize, pathPrefix);
        this.resources = resources;
        this.name = name;
        this.dest = dest;
    }

    public Iterator<Resource> iterator() {
        final Iterator<Node> base;

        base = resources.iterator();
        return new Iterator<Resource>() {
            public boolean hasNext() {
                return base.hasNext();
            }

            public Resource next() {
                Node file;

                file = base.next();
                try {
                    return new Resource(file.readBytes(), file.getRelative(dest), name);
                } catch (IOException e) {
                    throw new RuntimeException("TODO", e);
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}