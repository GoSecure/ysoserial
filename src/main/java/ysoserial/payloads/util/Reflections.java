package ysoserial.payloads.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class Reflections {

    public static Field getField(final Class<?> clazz, final String fieldName) throws Exception {
        Class topClazz = clazz;
        Field field = null;
        while (topClazz != null) {
            try {
                field = topClazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
            }
            topClazz = topClazz.getSuperclass();
        }
        if (field == null) {
            throw new NoSuchFieldException(clazz.getSimpleName() + " does not have the field " + fieldName + "");
        }
        field.setAccessible(true);
        return field;
    }

	public static void setFieldValue(final Object obj, final String fieldName, final Object value) throws Exception {
		final Field field = getField(obj.getClass(), fieldName);
		field.set(obj, value);
	}

	public static Object getFieldValue(final Object obj, final String fieldName) throws Exception {
		final Field field = getField(obj.getClass(), fieldName);		
		return field.get(obj);
	}

	public static Constructor<?> getFirstCtor(final String name) throws Exception {
		final Constructor<?> ctor = Class.forName(name).getDeclaredConstructors()[0];
	    ctor.setAccessible(true);
	    return ctor;
	}

}
