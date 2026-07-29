package com.lastkey.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileResourceConfig implements WebMvcConfigurer {

    private final Path profileImagesDirectory;

    public FileResourceConfig(
            @Value("${app.upload.profile-images-dir}")
            String profileImagesDirectory
    ) {

        if (profileImagesDirectory == null
                || profileImagesDirectory.isBlank()) {

            throw new IllegalStateException(
                    "Property 'app.upload.profile-images-dir' is missing"
            );
        }

        this.profileImagesDirectory =
                Paths.get(profileImagesDirectory)
                        .toAbsolutePath()
                        .normalize();
    }

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry
    ) {

        String resourceLocation =
                profileImagesDirectory
                        .toUri()
                        .toString();

        registry
                .addResourceHandler(
                        "/uploads/profile-images/**"
                )
                .addResourceLocations(
                        resourceLocation
                )
                .setCachePeriod(3600);
    }
}