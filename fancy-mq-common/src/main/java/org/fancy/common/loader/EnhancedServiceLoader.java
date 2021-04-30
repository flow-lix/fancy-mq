package org.fancy.common.loader;

public class EnhancedServiceLoader {

    public static <T> T load(Class<T> clazz, String name) {
        try {
            Class<?> tClass = Class.forName(name, true, findClassLoader());
            return clazz.cast(tClass.newInstance());
        } catch (Exception e) {
            throw new IllegalStateException("Class: " + clazz + ", 不能被实例化");
        }
    }

    private static ClassLoader findClassLoader() {
        return EnhancedServiceLoader.class.getClassLoader();
    }

}
