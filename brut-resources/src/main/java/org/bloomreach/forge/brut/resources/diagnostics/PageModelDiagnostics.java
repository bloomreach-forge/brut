package org.bloomreach.forge.brut.resources.diagnostics;

import org.bloomreach.forge.brut.resources.pagemodel.PageComponent;
import org.bloomreach.forge.brut.resources.pagemodel.PageModelResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Diagnostic utilities for analyzing PageModel API responses and HST configuration issues.
 * Provides detailed diagnostic information when pages or components are not found.
 */
public final class PageModelDiagnostics {

    private PageModelDiagnostics() {
    }

    /**
     * Diagnoses why a page is not loading as expected.
     * Checks if the actual page matches the expected page and provides recommendations.
     *
     * @param expectedPage the expected page name
     * @param requestPath  the request path
     * @param pageModel    the PageModel response
     * @return diagnostic result with severity and recommendations
     */
    public static DiagnosticResult diagnosePageNotFound(
            String expectedPage,
            String requestPath,
            PageModelResponse pageModel) {

        if (pageModel == null) {
            return DiagnosticResult.error(
                "PageModel is null - no response received",
                List.of("Verify that the HST request is properly configured",
                        "Check that the filter chain executed successfully")
            );
        }

        PageComponent rootComponent = pageModel.getRootComponent();
        if (rootComponent == null) {
            return DiagnosticResult.error(
                "No root component found in PageModel",
                List.of("Check that the PageModel response is properly structured",
                        "Verify that the 'root' reference exists in the response")
            );
        }

        String actualPage = rootComponent.getName();

        if (expectedPage.equals(actualPage)) {
            return DiagnosticResult.success(
                String.format("Page '%s' loaded successfully", expectedPage)
            );
        }

        if ("pagenotfound".equals(actualPage)) {
            return diagnosePageNotFoundScenario(expectedPage, requestPath, pageModel);
        }

        return DiagnosticResult.error(
            String.format("Expected page '%s' but got '%s'", expectedPage, actualPage),
            List.of("Verify the sitemap configuration for path: " + requestPath,
                    "Check that the sitemap item's hst:componentconfigurationid is set to: hst:pages/" + expectedPage,
                    "Current response resolved to page '" + actualPage + "' - the sitemap entry may be pointing to the wrong page")
        );
    }

    private static DiagnosticResult diagnosePageNotFoundScenario(
            String expectedPage,
            String requestPath,
            PageModelResponse pageModel) {

        List<String> recommendations = new ArrayList<>();

        recommendations.add(String.format(
            "Add sitemap entry in: hst/configurations/<channel>/sitemap.yaml for path: %s",
            requestPath.isEmpty() ? "/" : requestPath
        ));

        recommendations.add(String.format(
            "Ensure sitemap entry points to: hst:pages/%s", expectedPage
        ));

        recommendations.add(String.format(
            "Create page configuration in: hst/configurations/<channel>/pages/%s.yaml",
            expectedPage
        ));

        recommendations.add("Example sitemap entry:\n" +
            "   /" + (requestPath.isEmpty() ? "root" : requestPath) + ":\n" +
            "     jcr:primaryType: hst:sitemapitem\n" +
            "     hst:componentconfigurationid: hst:pages/" + expectedPage
        );

        if (pageModel.getPage() != null) {
            long componentCount = pageModel.getPage().values().stream()
                .filter(c -> !"pagenotfound".equals(c.getName()))
                .count();

            if (componentCount > 0) {
                recommendations.add(String.format(
                    "Found %d other component(s) in PageModel - check if sitemap is using correct page reference",
                    componentCount
                ));
            }
        }

        return DiagnosticResult.error(
            String.format("Expected page '%s' but got 'pagenotfound'", expectedPage),
            recommendations
        );
    }

    /**
     * Diagnoses why a component is not found in the PageModel.
     * Provides list of available components and recommendations.
     *
     * @param componentName the expected component name
     * @param pageModel     the PageModel response
     * @return diagnostic result with severity and recommendations
     */
    public static DiagnosticResult diagnoseComponentNotFound(
            String componentName,
            PageModelResponse pageModel) {
        return diagnoseComponentNotFound(componentName, null, pageModel);
    }

    /**
     * Diagnoses why a component is not found in the PageModel.
     * When the page map is empty, uses the request URI to give targeted sitemap advice.
     *
     * @param componentName the expected component name
     * @param requestUri    the full request URI (e.g. /site/fpp/resourceapi), used to extract sitemap path
     * @param pageModel     the PageModel response
     * @return diagnostic result with severity and recommendations
     */
    public static DiagnosticResult diagnoseComponentNotFound(
            String componentName,
            String requestUri,
            PageModelResponse pageModel) {

        if (pageModel == null || pageModel.getPage() == null || pageModel.getPage().isEmpty()) {
            return diagnoseMissingSitemapEntry(requestUri);
        }

        Optional<PageComponent> component = pageModel.findComponentByName(componentName);
        if (component.isPresent()) {
            return DiagnosticResult.success(
                String.format("Component '%s' found successfully", componentName)
            );
        }

        List<String> recommendations = new ArrayList<>();

        List<String> availableComponents = pageModel.getPage().values().stream()
            .map(PageComponent::getName)
            .filter(name -> name != null && !name.isEmpty())
            .distinct()
            .sorted()
            .toList();

        if (availableComponents.isEmpty()) {
            recommendations.add("No components with names found in the PageModel");
            recommendations.add("Check that component definitions include 'name' property");
        } else {
            recommendations.add("Available components in PageModel: " +
                String.join(", ", availableComponents));
            recommendations.add("Verify component name spelling and case sensitivity");
        }

        recommendations.add("Check container references (hst:referencecomponent paths)");
        recommendations.add("Verify workspace container definitions have child components");

        return DiagnosticResult.error(
            String.format("Component '%s' not found in PageModel", componentName),
            recommendations
        );
    }

    /**
     * Diagnoses an empty HTTP response from a Page Model API request.
     * An empty body typically means HST encountered a ContainerException, most commonly
     * caused by a component class that is not loadable in the test classpath.
     *
     * @param requestUri the full request URI (e.g. /site/fr/resourceapi)
     * @return diagnostic result with severity and recommendations
     */
    public static DiagnosticResult diagnoseEmptyResponse(String requestUri) {
        List<String> recommendations = new ArrayList<>();

        recommendations.add("Look for 'Component class not loadable' warnings in the test output above");
        recommendations.add("A component class referenced in workspace YAML is not on the test classpath");
        recommendations.add("Check hst:componentclassname values in hst:workspace/hst:containers/ YAML files");
        recommendations.add("If the component is from a separate module, add it as a test dependency");
        recommendations.add("Alternatively, remove or stub the component class reference in workspace YAML for tests");

        String message = requestUri != null
            ? String.format("Empty response from '%s' - component class loading failure likely", requestUri)
            : "Empty Page Model response - component class loading failure likely";

        return DiagnosticResult.error(message, recommendations);
    }

    private static DiagnosticResult diagnoseMissingSitemapEntry(String requestUri) {
        List<String> recommendations = new ArrayList<>();

        if (requestUri != null) {
            String sitemapPath = stripResourceApiSuffix(requestUri);
            recommendations.add(String.format(
                "Verify a sitemap item exists for '%s' in hst/configurations/<channel>/hst:sitemap/",
                sitemapPath
            ));
        } else {
            recommendations.add("Verify a sitemap item exists for the requested path in hst/configurations/<channel>/hst:sitemap/");
        }

        recommendations.add("A missing sitemap item causes HST to not recognize the request as a Page Model API call");
        recommendations.add("Ensure the sitemap item has: hst:componentconfigurationid: hst:pages/<page-name>");

        String message = requestUri != null
            ? String.format("No Page Model response for '%s' - likely a missing or misconfigured sitemap entry", requestUri)
            : "No components found in PageModel - page map is empty, likely a missing sitemap entry";

        return DiagnosticResult.error(message, recommendations);
    }

    private static String stripResourceApiSuffix(String requestUri) {
        int suffixIndex = requestUri.lastIndexOf("/resourceapi");
        return suffixIndex >= 0 ? requestUri.substring(0, suffixIndex) : requestUri;
    }

    /**
     * Diagnoses why a container is empty (has no children).
     * Checks if the container exists and analyzes its structure.
     *
     * @param containerName the container name to check
     * @param pageModel     the PageModel response
     * @return diagnostic result with severity and recommendations
     */
    public static DiagnosticResult diagnoseEmptyContainer(
            String containerName,
            PageModelResponse pageModel) {

        if (pageModel == null || pageModel.getPage() == null) {
            return DiagnosticResult.error(
                "PageModel is null or has no page map",
                List.of("Cannot diagnose container - verify PageModel response is valid")
            );
        }

        Optional<PageComponent> containerOpt = pageModel.findComponentByName(containerName);
        if (containerOpt.isEmpty()) {
            List<String> availableContainers = pageModel.getPage().values().stream()
                .filter(PageComponent::isContainer)
                .map(PageComponent::getName)
                .filter(name -> name != null)
                .distinct()
                .sorted()
                .toList();

            List<String> recommendations = new ArrayList<>();
            if (!availableContainers.isEmpty()) {
                recommendations.add("Available containers: " + String.join(", ", availableContainers));
            } else {
                recommendations.add("No containers found in PageModel");
            }
            recommendations.add("Verify container name and page configuration");

            return DiagnosticResult.error(
                String.format("Container '%s' not found in PageModel", containerName),
                recommendations
            );
        }

        PageComponent container = containerOpt.get();
        List<PageComponent> children = pageModel.getChildComponents(container);

        if (!children.isEmpty()) {
            return DiagnosticResult.success(
                String.format("Container '%s' has %d child component(s)", containerName, children.size())
            );
        }

        List<String> recommendations = new ArrayList<>();
        recommendations.add("Ensure container uses hst:containercomponentreference type");
        recommendations.add("Check that hst:referencecomponent points to valid workspace path");
        recommendations.add("Example: hst:referencecomponent: hst:workspace/hst:containers/" + containerName);
        recommendations.add("Add child components to workspace container definition");
        recommendations.add("Verify workspace.yaml has container with child component definitions");

        String containerType = container.getType();
        if (containerType != null && !containerType.contains("container")) {
            recommendations.add(0, String.format(
                "NOTE: Container type is '%s' - expected 'container' or 'containercomponentreference'",
                containerType
            ));
        }

        return DiagnosticResult.warning(
            String.format("Container '%s' is empty (no child components)", containerName),
            recommendations
        );
    }
}
