/**
 * HST Configuration Diagnostics for BRUT.
 *
 * <p>This package provides diagnostic capabilities for troubleshooting HST configuration issues
 * in Bloomreach Experience Manager tests. When tests fail due to missing pages, components, or
 * empty containers, the diagnostics provide actionable recommendations.
 *
 * <h2>Quick Start</h2>
 *
 * <h3>Option 1: Fluent Assertions (Recommended)</h3>
 * <pre>{@code
 * PageModelResponse pm = PageModelResponse.parse(jsonString);
 * PageModelAssert.assertThat(pm, "/", "landing")
 *     .hasPage("homepage")              // fails with diagnostics if pagenotfound
 *     .hasComponent("banner")           // fails with diagnostics if missing
 *     .containerNotEmpty("main");       // fails with diagnostics if empty
 * }</pre>
 *
 * <h3>Option 2: Manual Diagnostics</h3>
 * <pre>{@code
 * DiagnosticResult result = PageModelDiagnostics.diagnosePageNotFound(
 *     "homepage", "/", pageModel
 * );
 * System.out.println(result);  // Formatted diagnostic output
 * }</pre>
 *
 * <h2>Diagnostic Capabilities</h2>
 *
 * <h3>1. Page Not Found</h3>
 * <p>Diagnoses why a page resolves to "pagenotfound":
 * <ul>
 *   <li>Checks sitemap configuration</li>
 *   <li>Validates page references</li>
 *   <li>Suggests correct YAML configuration</li>
 * </ul>
 *
 * <h3>2. Component Missing</h3>
 * <p>Diagnoses why a component isn't found:
 * <ul>
 *   <li>Lists available components</li>
 *   <li>Checks container references</li>
 *   <li>Validates workspace definitions</li>
 * </ul>
 *
 * <h3>3. Empty Container</h3>
 * <p>Diagnoses why a container has no children:
 * <ul>
 *   <li>Validates containercomponentreference usage</li>
 *   <li>Checks workspace container definitions</li>
 *   <li>Suggests correct reference paths</li>
 * </ul>
 *
 * <h2>Example Output</h2>
 *
 * <p>When a test fails, diagnostics provide:
 * <pre>
 * [ERROR] Expected page 'homepage' but got 'pagenotfound'
 *
 * RECOMMENDATIONS:
 *    • Add sitemap entry in: hst/configurations/landing/sitemap.yaml for path: /
 *    • Ensure sitemap entry points to: hst:pages/homepage
 *    • Create page configuration in: hst/configurations/landing/pages/homepage.yaml
 *    • Example sitemap entry:
 *      /root:
 *        jcr:primaryType: hst:sitemapitem
 *        hst:componentconfigurationid: hst:pages/homepage
 * </pre>
 *
 * <h2>Integration with Existing Tests</h2>
 *
 * <p>Diagnostics integrate seamlessly with existing BRUT tests:
 * <pre>{@code
 * // Before: Standard assertion
 * assertTrue(pageModel.findComponentByName("banner").isPresent());
 *
 * // After: Enhanced assertion with diagnostics
 * PageModelAssert.assertThat(pageModel).hasComponent("banner");
 * }</pre>
 *
 * @see PageModelAssert
 * @see PageModelDiagnostics
 * @see DiagnosticResult
 */
package org.bloomreach.forge.brut.resources.diagnostics;
