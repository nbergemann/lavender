/*
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
package net.oneandone.lavender.index;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LabelTest {
    private Label label;

    @BeforeEach
    public void setup() {
        byte[] data = new byte[] { 0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0x81, (byte) 0xFF };
        byte[] md5 = Util.md5(data);
        String md5hex = Hex.encodeString(md5);
        label = new Label("modules/x/img/close.gif", "x/" + md5hex + "-close.gif", md5);
    }

    @Test
    public void testGetOriginalPath() {
        assertEquals("modules/x/img/close.gif", label.getOriginalPath());
    }

    @Test
    public void testGetLavendelizedPath() {
        String lavendelizedPath = label.getLavendelizedPath();
        assertNotNull(lavendelizedPath);
        assertEquals("x/852e7d76cdb8af7395cd039c0ecc293a-close.gif", lavendelizedPath);
    }

    @Test
    public void testHash() {
        byte[] md5 = label.md5();
        assertNotNull(md5);
        assertEquals(16, md5.length);
        assertEquals("852e7d76cdb8af7395cd039c0ecc293a", Hex.encodeString(md5));
    }

    @Test
    public void testToString() {
        String string = label.toString();
        assertNotNull(string);
    }

}
