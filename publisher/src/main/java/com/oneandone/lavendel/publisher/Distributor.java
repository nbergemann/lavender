package com.oneandone.lavendel.publisher;

import com.oneandone.lavendel.index.Index;
import com.oneandone.lavendel.index.Label;
import com.oneandone.lavendel.publisher.config.Cluster;
import com.oneandone.lavendel.publisher.config.Host;
import com.oneandone.lavendel.publisher.config.Net;
import com.oneandone.lavendel.publisher.config.Vhost;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Receives extracted files and uploads them */
public class Distributor {
    public static Distributor forCdn(World world, Cluster cluster, Vhost vhost, String indexName) throws IOException {
        return open(world, vhost.docroot, cluster.hosts, indexName);
    }

    public static Distributor forTest(FileNode baseDirectory, String indexName) throws IOException {
        Cluster cluster;
        Vhost vhost;

        cluster = new Cluster();
        cluster.hosts.add(Net.local(baseDirectory, baseDirectory.join("index.idx")));
        vhost = Vhost.one("dummy");
        cluster.vhosts.add(vhost);
        return Distributor.forCdn(baseDirectory.getWorld(), cluster, vhost, indexName);
    }

    public static Distributor open(World world, String suffix, List<Host> hosts, String indexName) throws IOException {
        Node root;
        Node destroot;
        Node index;
        Map<Node, Node> targets;
        Index prev;
        Index tmp;

        targets = new LinkedHashMap<>(); // to preserve order
        prev = null;
        for (Host host : hosts) {
            root = host.open(world);
            destroot = host.docroot(root, suffix);
            index = host.index(root, indexName);
            if (index.exists()) {
                try (InputStream src = index.createInputStream()) {
                    tmp = new Index(src);
                }
            } else {
                tmp = new Index();
            }
            if (prev == null) {
                prev = tmp;
            } else {
                if (!prev.equals(tmp)) {
                    throw new IOException("index mismatch");
                }
            }
            targets.put(index, destroot);
        }
        if (prev == null) {
            // no hosts!
            prev = new Index();
        }
        return new Distributor(targets, prev);
    }

    /** left: index location; right: docroot */
    private final Map<Node, Node> targets;
    private final Index prev;
    private final Index next;

    public Distributor() {
        this(new HashMap<Node, Node>(), new Index());
    }

    public Distributor(Map<Node, Node> targets, Index prev) {
        this.targets = targets;
        this.prev = prev;
        this.next = new Index();
    }

    public boolean write(Label label, byte[] data) throws IOException {
        Node dest;
        Label prevLabel;
        boolean changed;

        prevLabel = prev.lookup(label.getOriginalPath());
        if (prevLabel == null) {
            // add
            for (Node destroot : targets.values()) {
                dest = destroot.join(label.getLavendelizedPath());
                dest.getParent().mkdirsOpt();
                dest.writeBytes(data);
            }
            changed = true;
        } else if (!Arrays.equals(prevLabel.md5(), label.md5())) {
            // update
            for (Node destroot : targets.values()) {
                destroot.join(label.getLavendelizedPath()).writeBytes(data);
            }
            changed = true;
        } else {
            changed = false;
        }
        next.add(label);
        return changed;
    }

    /** return next index */
    public Index close() throws IOException {
        Node index;

        if (next.size() == 0) {
            return next;
        }
        for (Map.Entry<Node, Node> entry : targets.entrySet()) {
            index = entry.getKey();
            index.getParent().mkdirOpt();
            try (OutputStream out = index.createOutputStream()) {
                next.save(out);
            }
        }
        return next;
    }
}
