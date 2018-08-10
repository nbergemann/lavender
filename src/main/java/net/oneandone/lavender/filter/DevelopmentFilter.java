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
package net.oneandone.lavender.filter;

import net.oneandone.lavender.config.SystemProperties;
import net.oneandone.lavender.modules.NodeModule;
import net.oneandone.lavender.modules.Module;
import net.oneandone.lavender.modules.Resource;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.xml.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Created by awiegant on 04.03.16.
 */
public class DevelopmentFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(ProductionFilter.class);

    FilterConfig filterConfig;

    World world;
    protected List<Module> modules;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        long started = System.currentTimeMillis();
        SystemProperties properties = null;
        Node webapp;

        this.filterConfig = filterConfig;

        try {
            world = World.create(false);
            webapp = world.file(filterConfig.getServletContext().getRealPath(""));
            properties = SystemProperties.load(SystemProperties.file(world), false);
            FileNode cache = properties.lockedCache(5, "lavenderServlet");
            modules = loadModulesFromWebapp(webapp, properties, cache);
        } catch (XmlException | IOException | SAXException e) {
            e.printStackTrace();
            throw new ServletException("Could not initialize Lavender development filter", e);
        } finally {
            unlockPropertiesCache(properties);
        }
        LOG.info("Lavender development filter for " + webapp + ", " + modules.size()
                + " resources. Init in " + (System.currentTimeMillis() - started + " ms"));

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (!intercept(request, response)) {
            chain.doFilter(request, response);
            LOG.debug("[passed through: " + request.getMethod() + " " + request.getRequestURI() + ": " + response.getStatus() + "]");
        }
    }

    @Override
    public void destroy() {
        // nothing to do
    }

    public int getModulesCount() {
        return modules.size();
    }

    List<Module> loadModulesFromWebapp(Node webapp, SystemProperties properties, FileNode cache)
            throws IOException, SAXException, XmlException {
        return NodeModule.fromWebapp(cache, false, webapp, properties.svnUsername, properties.svnPassword);
    }

    private void unlockPropertiesCache(SystemProperties properties) {
        if (properties != null) {
            try {
                properties.unlockCache();
            } catch (IOException e) {
                LOG.error("Could not unlock properties cache", e);
            }
        }
    }

    public boolean intercept(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path;
        Resource resource;

        path = request.getPathInfo();
        if (path == null || !path.startsWith("/")) {
            return false;
        }
        path = path.substring(1);
        resource = lookup(path);
        if (resource == null) {
            return false;
        }

        switch (request.getMethod()) {
            case "GET":
                doGetRequest(resource, request, response, true);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(response.getStatus() + " GET " + path + " -> " + resource.getOrigin());
                }
                return true;
            case "HEAD":
                doGetRequest(resource, request, response, false);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(response.getStatus() + " HEAD " + path + " -> " + resource.getOrigin());
                }
                return true;
            default:
                return false;
        }
    }

    private synchronized Resource lookup(String resourcePath) throws IOException {
        Resource resource;

        // lookup cached stuff first
        for (Module module : modules) {
            if (module.hasScan()) {
                resource = module.probe(resourcePath);
                if (resource != null) {
                    if (resource.isOutdated()) {
                        LOG.info(resource.getOrigin() + ": outdated");
                    } else {
                        return resource;
                    }
                }
            }
        }
        for (Module module : modules) {
            if (module.matches(resourcePath) != null) {
                module.softInvalidateScan();
                resource = module.probe(resourcePath);
                if (resource != null) {
                    return resource;
                }
            }
        }
        return null;
    }


    public void doGetRequest(Resource resource, HttpServletRequest request, HttpServletResponse response, boolean withBody) throws IOException {
        String etag;
        String contentType;
        ServletOutputStream out;
        byte[] data;
        String previousEtag;

        etag = etag(resource.getContentId());
        response.setHeader("ETag", etag);
        contentType = filterConfig.getServletContext().getMimeType(resource.getPath());
        if (contentType != null) {
            response.setContentType(contentType);
        }

        previousEtag = request.getHeader("If-None-Match");
        if (etag.equals(previousEtag)) {
            LOG.debug("ETag match: returning 304 Not Modified: " + resource.getPath());
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
        } else  { 		// first time through - set last modified time to now
            data = resource.getData();
            if (withBody) {
                response.setContentLength(data.length);
                out = response.getOutputStream();
                try {
                    response.setBufferSize(4096);
                } catch (IllegalStateException e) {
                    // Silent catch
                }
                out.write(data);
            }
        }
    }

    private static String etag(String str) {
        if (str.contains("\"")) {
            throw new UnsupportedOperationException("TODO: " + str);
        }
        return '"' + str + '"';
    }

}
