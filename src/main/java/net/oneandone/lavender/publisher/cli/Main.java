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
package net.oneandone.lavender.publisher.cli;

import com.jcraft.jsch.JSchException;
import net.oneandone.lavender.publisher.config.Net;
import net.oneandone.sushi.cli.Child;
import net.oneandone.sushi.cli.Cli;
import net.oneandone.sushi.cli.Command;
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.ssh.SshFilesystem;
import net.oneandone.sushi.fs.svn.SvnFilesystem;

import java.io.IOException;

public class Main extends Cli implements Command {
    public static void main(String[] args) throws IOException {
        System.exit(doMain(true, Net.normal(), null, args));
    }

    public static int doMain(boolean withSsl, Net net, String logspath, String... args) throws IOException {
        Main main;

        main = new Main(withSsl, net, logspath);
        return main.run(args);
    }

    public static World world(boolean withSsh, String logspath) throws MkdirException {
        World world;
        SshFilesystem ssh;
        FileNode logs;

        world = new World();
        if (logspath == null) {
            logs = (FileNode) world.getHome().join("logs/lavender");
        } else {
            logs = world.file(logspath);
        }
        // /var/log is too small on pumamma64
        world.setTemp((FileNode) logs.mkdirOpt());

        if (withSsh) {
            world.getFilesystem("svn", SvnFilesystem.class).setDefaultCredentials("pfixpublisher", "pfixpublisher");
            ssh = world.getFilesystem("ssh", SshFilesystem.class);
            try {
                ssh.addIdentity(world.resource("id_dsa"), null);  // flash access
                ssh.addDefaultIdentity();                         // sftp.1und1.de in bazaar command
            } catch (JSchException | IOException e) {
                throw new IllegalStateException("cannot connect to flash server: " + e.getMessage(), e);
            }
        } else {
            // disable them for integration tests, because I don't have .ssh on pearl/gems
        }
        return world;
    }


    private final Net net;

    public Main(boolean withSsl, Net net, String logs) throws MkdirException {
        super(world(withSsl, logs));
        this.net = net;
    }

    @Child("war")
    public Command war() {
        return new War(console, net);
    }

    @Child("svn")
    public Command svn() {
        return new Svn(console, net);
    }

    @Child("direct")
    public Command direct() {
        return new Direct(console, net);
    }

    @Child("bazaar")
    public Command bazaar() {
        return new Bazaar(console, net);
    }

    @Child("gc")
    public Command gc() {
        return new GarbageCollection(console, net);
    }

    @Override
    public void printHelp() {
        console.info.println("usage: 'lavender' command");
        console.info.println();
        console.info.println("publishing commands");
        console.info.println("  'war' cluster inputWar outputWar idxName");
        console.info.println("                            publish resources from the specified war.");
        console.info.println("  'svn' directory           publish resources from svn");
        console.info.println("other commands");
        console.info.println("  'help'                    print this message");
        console.info.println("  'version'                 print version information");
        console.info.println("  'bazaar'                  publishes Bazaar Voice files to eu cluster.");
        console.info.println("                            actually publish to the specified cluster");
        console.info.println("  'direct' arg+             executes the specified command on all hosts");
        console.info.println("  'gc' ['-dryrun'] cluster  removes unreferenced resources and empty directories from the");
        console.info.println("                            specified host(s); default: all hosts");
    }

    @Override
    public void invoke() {
        printHelp();
    }
}