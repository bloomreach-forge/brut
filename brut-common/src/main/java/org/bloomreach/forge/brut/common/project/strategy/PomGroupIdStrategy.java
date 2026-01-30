package org.bloomreach.forge.brut.common.project.strategy;

import org.bloomreach.forge.brut.common.project.ProjectDiscovery.BeanPackageOrder;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Strategy that derives bean packages from the pom.xml groupId.
 * Generates packages: [groupId].beans, [groupId].model, optionally [groupId].domain
 * Priority: 30
 *
 * <p>Handles parent pom inheritance by checking for both groupId and parent/groupId.</p>
 */
public final class PomGroupIdStrategy implements BeanPackageStrategy {

    public static final int PRIORITY = 30;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public Optional<List<String>> resolve(DiscoveryContext context) {
        Optional<Path> projectRootOpt = context.getProjectRoot();
        if (projectRootOpt.isEmpty()) {
            return Optional.empty();
        }

        Path pomPath = projectRootOpt.get().resolve("pom.xml");
        if (!Files.exists(pomPath)) {
            return Optional.empty();
        }

        String groupId = parseGroupId(pomPath);
        if (groupId == null || groupId.isBlank()) {
            return Optional.empty();
        }

        Set<String> packages = new LinkedHashSet<>();

        if (context.getOrder() == BeanPackageOrder.MODEL_FIRST) {
            packages.add(groupId + ".model");
            packages.add(groupId + ".beans");
        } else {
            packages.add(groupId + ".beans");
            packages.add(groupId + ".model");
        }

        if (context.isIncludeDomain()) {
            packages.add(groupId + ".domain");
        }

        return Optional.of(new ArrayList<>(packages));
    }

    /**
     * Parses groupId from pom.xml. Checks direct groupId first, then parent groupId.
     */
    String parseGroupId(Path pomPath) {
        try (BufferedReader reader = Files.newBufferedReader(pomPath, StandardCharsets.UTF_8)) {
            String directGroupId = null;
            String parentGroupId = null;
            boolean inParent = false;
            int parentDepth = 0;

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                // Track parent section
                if (trimmed.startsWith("<parent>") || trimmed.equals("<parent>")) {
                    inParent = true;
                    parentDepth = 1;
                    continue;
                }
                if (inParent) {
                    if (trimmed.contains("<parent")) {
                        parentDepth++;
                    }
                    if (trimmed.contains("</parent>")) {
                        parentDepth--;
                        if (parentDepth == 0) {
                            inParent = false;
                        }
                    }
                }

                // Extract groupId
                if (trimmed.startsWith("<groupId>") && trimmed.endsWith("</groupId>")) {
                    String value = trimmed.replace("<groupId>", "").replace("</groupId>", "").trim();
                    if (!value.contains("$")) {  // Skip property references
                        if (inParent) {
                            parentGroupId = value;
                        } else if (directGroupId == null) {
                            directGroupId = value;
                        }
                    }
                }

                // Stop after artifactId (groupId always comes before dependencies)
                if (trimmed.startsWith("<dependencies>") || trimmed.startsWith("<modules>")) {
                    break;
                }
            }

            return directGroupId != null ? directGroupId : parentGroupId;
        } catch (IOException e) {
            return null;
        }
    }
}
