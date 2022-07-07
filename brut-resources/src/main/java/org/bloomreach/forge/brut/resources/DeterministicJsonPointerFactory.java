package org.bloomreach.forge.brut.resources;

import org.hippoecm.hst.pagemodelapi.v10.core.container.JsonPointerFactoryImpl;

public class DeterministicJsonPointerFactory extends JsonPointerFactoryImpl {

    private static ThreadLocal<DeterministicJsonPointerFactory> tlDeterministicJsonPointerFactoryHolder = new ThreadLocal<>();

    public static DeterministicJsonPointerFactory get() {
        reset();
        return tlDeterministicJsonPointerFactoryHolder.get();
    }

    private static void set(DeterministicJsonPointerFactory jsonPointerFactory) {
        tlDeterministicJsonPointerFactoryHolder.set(jsonPointerFactory);
    }

    private long id = 0;

    public static void reset() {
        tlDeterministicJsonPointerFactoryHolder.set(new DeterministicJsonPointerFactory());
    }

    @Override
    public String createJsonPointerId() {
        return createJsonPointerIdForString("id" + get().id++);
    }
}