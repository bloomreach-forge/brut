package org.bloomreach.forge.brut.resources.diagnostics;

import org.bloomreach.forge.brut.resources.pagemodel.PageComponent;
import org.bloomreach.forge.brut.resources.pagemodel.PageModelResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Fluent assertions for PageModel with integrated diagnostics.
 * Provides readable assertions that include detailed diagnostic information on failure.
 *
 * <p>Example usage:
 * <pre>
 * PageModelResponse pm = PageModelResponse.parse(jsonString);
 * PageModelAssert.assertThat(pm, "/", "landing")
 *     .hasPage("homepage")
 *     .hasComponent("banner")
 *     .containerNotEmpty("main");
 * </pre>
 */
public class PageModelAssert {

    private final PageModelResponse pageModel;
    private final String requestPath;
    private final String channel;

    private PageModelAssert(PageModelResponse pageModel, String requestPath, String channel) {
        this.pageModel = pageModel;
        this.requestPath = requestPath;
        this.channel = channel;
    }

    /**
     * Creates a PageModelAssert for the specified PageModel response.
     *
     * @param pageModel   the PageModel response to assert on
     * @param requestPath the request path used
     * @param channel     the HST channel name
     * @return PageModelAssert for chaining
     */
    public static PageModelAssert assertThat(PageModelResponse pageModel, String requestPath, String channel) {
        return new PageModelAssert(pageModel, requestPath, channel);
    }

    /**
     * Creates a PageModelAssert with default path and channel.
     *
     * @param pageModel the PageModel response to assert on
     * @return PageModelAssert for chaining
     */
    public static PageModelAssert assertThat(PageModelResponse pageModel) {
        return new PageModelAssert(pageModel, "/", "unknown");
    }

    /**
     * Asserts that the PageModel loaded the expected page.
     * If the page doesn't match, fails with detailed diagnostics.
     *
     * @param expectedPage the expected page name
     * @return this for chaining
     */
    public PageModelAssert hasPage(String expectedPage) {
        DiagnosticResult result = PageModelDiagnostics.diagnosePageNotFound(
            expectedPage, requestPath, pageModel
        );

        if (result.severity() != DiagnosticSeverity.SUCCESS) {
            fail(result.toString());
        }

        return this;
    }

    /**
     * Asserts that a component exists in the PageModel.
     * If not found, fails with detailed diagnostics.
     *
     * @param componentName the component name to find
     * @return this for chaining
     */
    public PageModelAssert hasComponent(String componentName) {
        DiagnosticResult result = PageModelDiagnostics.diagnoseComponentNotFound(
            componentName, requestPath, pageModel
        );

        if (result.severity() != DiagnosticSeverity.SUCCESS) {
            fail(result.toString());
        }

        return this;
    }

    /**
     * Asserts that a component exists and returns it for further assertions.
     *
     * @param componentName the component name to find
     * @return the PageComponent for further inspection
     */
    public PageComponent getComponent(String componentName) {
        hasComponent(componentName);
        return pageModel.findComponentByName(componentName)
            .orElseThrow(() -> new AssertionError("Component should exist after hasComponent check"));
    }

    /**
     * Asserts that a container is not empty (has children).
     * If empty or not found, fails with detailed diagnostics.
     *
     * @param containerName the container name to check
     * @return this for chaining
     */
    public PageModelAssert containerNotEmpty(String containerName) {
        DiagnosticResult result = PageModelDiagnostics.diagnoseEmptyContainer(
            containerName, pageModel
        );

        if (result.severity() == DiagnosticSeverity.ERROR) {
            fail(result.toString());
        }

        if (result.severity() == DiagnosticSeverity.WARNING) {
            fail(result.toString());
        }

        return this;
    }

    /**
     * Asserts that a container exists and has at least the expected number of children.
     *
     * @param containerName     the container name
     * @param expectedMinChildren minimum expected number of children
     * @return this for chaining
     */
    public PageModelAssert containerHasMinChildren(String containerName, int expectedMinChildren) {
        Optional<PageComponent> containerOpt = pageModel.findComponentByName(containerName);

        if (containerOpt.isEmpty()) {
            DiagnosticResult result = PageModelDiagnostics.diagnoseEmptyContainer(
                containerName, pageModel
            );
            fail(result.toString());
        }

        PageComponent container = containerOpt.get();
        int actualChildren = pageModel.getChildComponents(container).size();

        if (actualChildren < expectedMinChildren) {
            fail(String.format(
                "Container '%s' has %d children, expected at least %d",
                containerName, actualChildren, expectedMinChildren
            ));
        }

        return this;
    }

    /**
     * Asserts that the PageModel has content items.
     *
     * @return this for chaining
     */
    public PageModelAssert hasContent() {
        if (!pageModel.hasContent()) {
            fail("PageModel has no content items");
        }
        return this;
    }

    /**
     * Asserts that a component has a specific model.
     *
     * @param componentName the component name
     * @param modelName     the model name to check
     * @return this for chaining
     */
    public PageModelAssert componentHasModel(String componentName, String modelName) {
        PageComponent component = getComponent(componentName);

        if (!component.hasModel(modelName)) {
            fail(String.format(
                "Component '%s' does not have model '%s'",
                componentName, modelName
            ));
        }

        return this;
    }

    /**
     * Gets the underlying PageModelResponse for advanced operations.
     *
     * @return the PageModelResponse
     */
    public PageModelResponse getPageModel() {
        return pageModel;
    }
}
