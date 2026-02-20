package org.bloomreach.forge.brut.resources;

import org.hippoecm.hst.pagemodelapi.v10.core.container.JsonPointerFactoryImpl;

public class DeterministicJsonPointerFactory extends JsonPointerFactoryImpl {

    private static ThreadLocal<DeterministicJsonPointerFactory> tlDeterministicJsonPointerFactoryHolder = new ThreadLocal<>();

    public static DeterministicJsonPointerFactory get() {
        DeterministicJsonPointerFactory factory = tlDeterministicJsonPointerFactoryHolder.get();
        if (factory == null) {
            factory = new DeterministicJsonPointerFactory();
            tlDeterministicJsonPointerFactoryHolder.set(factory);
        }
        return factory;
    }

    private long id = 0;

    public static void reset() {
        tlDeterministicJsonPointerFactoryHolder.set(new DeterministicJsonPointerFactory());
    }

    @Override
    public String createJsonPointerId() {
        DeterministicJsonPointerFactory factory = get();
        return createJsonPointerIdForString("id" + factory.id++);
    }
}
