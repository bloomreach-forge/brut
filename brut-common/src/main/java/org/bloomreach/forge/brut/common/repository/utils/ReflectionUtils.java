package org.bloomreach.forge.brut.common.repository.utils;

import javax.jcr.Session;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtils {

    private ReflectionUtils() {
        // utility class
    }

    public static Method getMethod(Object obj, String methodName, Class<?>... parameterTypes) {
        Method result;
        Class<?> type = obj.getClass();
        try {
            result = type.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            result = null;
        }
        return result;
    }

    public static Object invokeMethod(Method method, Object obj, Object... args) {
        try {
            return method.invoke(obj, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeReflectionException(e);
        }
    }

    public static Session unwrapSessionDecorator(Session session) {
        Session result = session;
        if (session != null && "org.hippoecm.repository.impl.SessionDecorator".equals(session.getClass().getName())) {
            Class<?> clazz = session.getClass();
            return (Session) getPrivateField(clazz, session, "session");
        }
        return result;
    }

    private static Object getPrivateField(Class<?> clazz, Object object, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeReflectionException(e);
        }
    }
}
