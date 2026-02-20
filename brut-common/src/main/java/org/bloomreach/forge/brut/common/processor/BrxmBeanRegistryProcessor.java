package org.bloomreach.forge.brut.common.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Annotation processor that writes a compile-time bean registry for BRUT.
 *
 * <p>Runs automatically at {@code test-compile} time whenever {@code brut-common} is
 * on the classpath (no additional configuration required). Produces two resource files
 * in the compiler output directory:
 *
 * <ul>
 *   <li>{@code META-INF/brut-beans.list} — FQNs of {@code @Node}-annotated classes
 *       found in the current compilation unit (typically {@code src/test/java} beans)</li>
 *   <li>{@code META-INF/brut-bean-packages.list} — bean package names extracted from
 *       {@code beanPackages} attributes on BRUT test annotations, replacing the
 *       {@code ProjectDiscovery} heuristic at runtime</li>
 * </ul>
 *
 * <p>Both files are optional at runtime — BRUT falls back to classpath scanning and
 * {@code ProjectDiscovery} when they are absent.
 */
@SupportedAnnotationTypes("org.hippoecm.hst.content.beans.Node")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class BrxmBeanRegistryProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        Set<String> beanFqns = new LinkedHashSet<>();

        for (TypeElement annotation : annotations) {
            collectNodeBeans(annotation, roundEnv, beanFqns);
        }

        if (!beanFqns.isEmpty()) {
            writeResource(BrxmBeanRegistry.BEANS_RESOURCE, beanFqns);
        }

        return false;
    }

    private void collectNodeBeans(TypeElement annotation, RoundEnvironment roundEnv,
                                  Set<String> beanFqns) {
        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
            if (element.getKind() == ElementKind.CLASS) {
                beanFqns.add(((TypeElement) element).getQualifiedName().toString());
            }
        }
    }

    private void writeResource(String resourcePath, Collection<String> lines) {
        try {
            FileObject file = processingEnv.getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", resourcePath);
            try (PrintWriter writer = new PrintWriter(file.openWriter())) {
                lines.forEach(writer::println);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "brut: could not write " + resourcePath + ": " + e.getMessage()
            );
        }
    }
}
