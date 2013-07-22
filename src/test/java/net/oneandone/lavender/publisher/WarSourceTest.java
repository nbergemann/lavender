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
package net.oneandone.lavender.publisher;

import net.oneandone.lavender.index.Index;
import net.oneandone.lavender.publisher.config.Filter;
import net.oneandone.lavender.publisher.config.Settings;
import net.oneandone.lavender.publisher.pustefix.PustefixSource;
import net.oneandone.sushi.fs.LineFormat;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Separator;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WarSourceTest {
    private WarEngine resourcePublisher;

    private FileNode war;

    private FileNode baseDirectory;

    private FileNode nodesFile;

    private Distributor distributor;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        World world;
        FileNode tmp;
        Settings settings;

        world = new World();
        settings = Settings.load(world);
        war = (FileNode) world.resource("dummy.war");
        tmp = world.getTemp().createTempDirectory();
        baseDirectory = tmp.join("storage").mkdir();
        nodesFile = tmp.join("nodes.lst");
        distributor = Distributor.forTest(baseDirectory, "notused");
        resourcePublisher = new WarEngine(settings.svnUsername, settings.svnPassword,
                war, war, distributor, new Index(), nodesFile, "");
    }

    @Test
    public void testRun() throws IOException {
        Collection<String> indexFileContent;
        Index index;

        resourcePublisher.extract(new PustefixSource(new Filter(), war.openZip()));
        index = distributor.close();
        indexFileContent = indexLines(index);
        assertEquals(1 + 9 + 1, indexFileContent.size()); // contains properties header line
        assertTrue(indexFileContent
                .contains("modules/frontend-tools/img/delete.png=684/61ca5187cd2c6af08786467085f2b/frontend-tools"
                        + "/delete.png\\:68461ca5187cd2c6af08786467085f2b"));
        assertTrue(indexFileContent
                .contains("modules/stageassistent/img/close.gif=d08/fcb52992b3e6da757c4e7778e70c1/stageassistent"
                        + "/close.gif\\:d08fcb52992b3e6da757c4e7778e70c1"));

        assertEquals(9 + 1, baseDirectory.list().size());
    }

    @Test
    public void testIncludedExcludes() throws IOException {
        Index index;
        Filter config = new Filter();
        config.setIncludes("*.jpg", "*.gif");
        config.setExcludes("**/close.gif");

        resourcePublisher.extract(new PustefixSource(config, war.openZip()));
        index = distributor.close();

        Collection<String> indexFileContent = indexLines(index);
        assertEquals(1 + 5 + 1, indexFileContent.size()); // contains properties header line
        assertFalse(indexFileContent
                .contains("modules/frontend-tools/img/delete.png=684/61ca5187cd2c6af08786467085f2b/frontend-tools"
                        + "/delete.png\\:68461ca5187cd2c6af08786467085f2b"));
        assertFalse(indexFileContent
                .contains("modules/stageassistent/img/close.gif=d08/fcb52992b3e6da757c4e7778e70c1/stageassistent"
                        + "/close.gif\\:d08fcb52992b3e6da757c4e7778e70c1"));

        assertEquals(5 + 1, baseDirectory.list().size());
    }

    public static final Separator LF = Separator.on("\n").trim(LineFormat.Trim.SEPARATOR);

    private List<String> indexLines(Index index) {
        StringWriter str;

        str = new StringWriter();
        try {
            index.save(str);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return LF.split(str.toString());
    }

}
