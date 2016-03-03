package ysoserial.payloads.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class PrintUtil {

    /**
     * Print the state of a given object.
     * @param obj
     * @throws IllegalAccessException
     */
    public static void printObject(Object obj) throws IllegalAccessException {
        System.out.println("===");
        printObject(obj,0);
        System.out.println("===");
    }

    private static void printObject(Object obj, int indent) throws IllegalAccessException {
        if(indent > 10) {
            System.out.println(repeat("\t",indent)+" !! Potential recursive reference");
            return;
        }
        if(obj instanceof Number || obj instanceof String || obj instanceof Boolean || obj instanceof Class) {
            return;
        }

        if(obj instanceof Serializable) {
            //System.out.println(StringUtils.repeat("\t",indent)+"[[Array "+obj.getClass()+"]]");
            if(obj.getClass().isArray()) {
                int length = Array.getLength(obj);
                if(length == 0) {
                    System.out.println(repeat("\t",indent)+" Empty array");
                }
                for (int i = 0; i < length; i ++) {
                    Object arrayElement = Array.get(obj, i);
                    System.out.println(repeat("\t",indent)+" ["+i+"] "+arrayElement);
                    printObject(arrayElement, indent + 1);
                }
            }
            else {
                System.out.println(repeat("\t",indent)+"{{"+obj.getClass()+"}}");
                for (Field field : getAllFields(obj.getClass())) {
                    if (!Modifier.isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        System.out.println(repeat("\t", indent) + "  - " + field.getName() + " (" + field.getType().getSimpleName() + ") = " + field.get(obj));
                        printObject(field.get(obj), indent + 1);
                    }

                }
            }
        }
    }

    /**
     * Get fields including the fields from the subclass.
     * @param clazz
     * @return
     */
    private static List<Field> getAllFields(Class clazz) {
        List<Field> allFields = new ArrayList<Field>();

        Class topClazz = clazz;
        while (topClazz != null) {
            for (Field f : topClazz.getDeclaredFields()) {
                allFields.add(f);
            }
            topClazz = topClazz.getSuperclass();
        }

        return allFields;
    }

    public static String repeat(String str, int repeat) {
        StringBuffer buf = new StringBuffer(str.length() * repeat);
        for (int i = 0; i < repeat; i++) {
            buf.append(str);
        }
        return buf.toString();
    }
}
