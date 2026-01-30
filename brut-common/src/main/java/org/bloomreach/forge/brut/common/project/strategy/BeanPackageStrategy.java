package org.bloomreach.forge.brut.common.project.strategy;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for discovering bean packages.
 * Implementations are ordered by priority (lower = higher priority).
 */
public interface BeanPackageStrategy {

    /**
     * Returns the priority of this strategy. Lower values are tried first.
     *
     * @return the priority value
     */
    int getPriority();

    /**
     * Attempts to resolve bean packages from the given context.
     *
     * @param context the discovery context
     * @return optional list of packages if this strategy can resolve them, empty otherwise
     */
    Optional<List<String>> resolve(DiscoveryContext context);

    /**
     * Returns a descriptive name for logging purposes.
     *
     * @return strategy name
     */
    default String getName() {
        return getClass().getSimpleName();
    }
}
