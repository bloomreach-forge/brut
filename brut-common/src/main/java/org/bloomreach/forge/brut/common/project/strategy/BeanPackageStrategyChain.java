package org.bloomreach.forge.brut.common.project.strategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Chains multiple {@link BeanPackageStrategy} implementations and executes them
 * in priority order until one returns a result.
 */
public final class BeanPackageStrategyChain {

    private final List<BeanPackageStrategy> strategies;

    private BeanPackageStrategyChain(List<BeanPackageStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * Creates a chain with the default strategies in priority order.
     *
     * @return new strategy chain
     */
    public static BeanPackageStrategyChain createDefault() {
        List<BeanPackageStrategy> strategies = new ArrayList<>();
        strategies.add(new ProjectSettingsStrategy());
        strategies.add(new ClasspathNodeAnnotationStrategy());
        strategies.add(new PomGroupIdStrategy());
        strategies.add(new TestClassHeuristicStrategy());
        strategies.sort(Comparator.comparingInt(BeanPackageStrategy::getPriority));
        return new BeanPackageStrategyChain(strategies);
    }

    /**
     * Creates a chain from ServiceLoader-discovered strategies.
     * Useful for extension via SPI.
     *
     * @return new strategy chain
     */
    public static BeanPackageStrategyChain fromServiceLoader() {
        List<BeanPackageStrategy> strategies = new ArrayList<>();
        ServiceLoader.load(BeanPackageStrategy.class).forEach(strategies::add);

        if (strategies.isEmpty()) {
            return createDefault();
        }

        strategies.sort(Comparator.comparingInt(BeanPackageStrategy::getPriority));
        return new BeanPackageStrategyChain(strategies);
    }

    /**
     * Creates a chain with custom strategies.
     *
     * @param strategies the strategies to use
     * @return new strategy chain
     */
    public static BeanPackageStrategyChain of(List<BeanPackageStrategy> strategies) {
        List<BeanPackageStrategy> sorted = new ArrayList<>(strategies);
        sorted.sort(Comparator.comparingInt(BeanPackageStrategy::getPriority));
        return new BeanPackageStrategyChain(sorted);
    }

    /**
     * Resolves bean packages by trying each strategy in priority order.
     *
     * @param context the discovery context
     * @return resolved packages, or empty list if no strategy succeeded
     */
    public List<String> resolve(DiscoveryContext context) {
        for (BeanPackageStrategy strategy : strategies) {
            Optional<List<String>> result = strategy.resolve(context);
            if (result.isPresent() && !result.get().isEmpty()) {
                return result.get();
            }
        }
        return List.of();
    }

    /**
     * Returns information about which strategies are in the chain.
     *
     * @return list of strategy names with priorities
     */
    public List<String> getStrategyInfo() {
        List<String> info = new ArrayList<>();
        for (BeanPackageStrategy strategy : strategies) {
            info.add(strategy.getName() + " (priority=" + strategy.getPriority() + ")");
        }
        return info;
    }
}
