package com.bloomreach.ps.brxm.jcr.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.cm.model.source.Source;

/**
 * Used for resolving relative resource paths. Used when importing images via yaml import facility.
 */

public class ImageResourceInputProvider implements ResourceInputProvider {

    private String yamlFilePath;

    public ImageResourceInputProvider(String yamlFilePath) {
        this.yamlFilePath = yamlFilePath;
    }

    @Override
    public boolean hasResource(final Source source, final String resourcePath) {
        if (resourcePath == null) {
            return false;
        }
        return getResourceFile(resourcePath).exists();
    }

    @Override
    public InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException {
        File resourceFile = getResourceFile(resourcePath);
        return Files.newInputStream(resourceFile.toPath());
    }

    private File getResourceFile(final String resourceRelPath) {
        File yamlFile = new File(yamlFilePath);
        File parentFile = yamlFile.getParentFile();
        return new File(parentFile.getAbsolutePath() + File.separator + resourceRelPath);
    }
}
