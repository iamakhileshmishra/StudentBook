package com.schoolerp.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * In the hosted build the React app is bundled into this jar (classpath:/static).
 * Existing files (index.html, /assets/*.js, images) are served as-is; any other
 * non-API path is a client-side route (/dashboard, /students/123), so it gets
 * index.html and React Router takes over. API paths never fall back.
 *
 * In local development the frontend runs on Vite instead and /static is empty,
 * so this has no effect there.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final Resource INDEX = new ClassPathResource("/static/index.html");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        Resource requested = location.createRelative(path);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (path.startsWith("api/") || path.startsWith("actuator/") || !INDEX.exists()) {
                            return null; // real 404 for API/actuator, or when no frontend is bundled
                        }
                        // A missing file (e.g. an old /assets/x.js after a redeploy) is a real 404;
                        // only extension-less paths are client-side routes
                        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
                        return lastSegment.contains(".") ? null : INDEX;
                    }
                });
    }
}
