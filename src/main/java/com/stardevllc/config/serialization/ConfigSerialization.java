package com.stardevllc.config.serialization;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigSerialization {
    public static final String SERIALIZED_TYPE_KEY = "==";
    private final Class<? extends ConfigSerializable> clazz;
    private static Map<String, Class<? extends ConfigSerializable>> aliases = new HashMap<>();

    protected ConfigSerialization(Class<? extends ConfigSerializable> clazz) {
        this.clazz = clazz;
    }

    protected Method getMethod(String name) {
        try {
            Method method = clazz.getDeclaredMethod(name, Map.class);

            if (!ConfigSerializable.class.isAssignableFrom(method.getReturnType())) {
                return null;
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                return null;
            }

            return method;
        } catch (NoSuchMethodException | SecurityException ex) {
            return null;
        }
    }

    protected Constructor<? extends ConfigSerializable> getConstructor() {
        try {
            return clazz.getConstructor(Map.class);
        } catch (NoSuchMethodException | SecurityException ex) {
            return null;
        }
    }

    protected ConfigSerializable deserializeViaMethod(Method method, Map<String, ?> args) {
        try {
            ConfigSerializable result = (ConfigSerializable) method.invoke(null, args);

            if (result == null) {
                Logger.getLogger(ConfigSerialization.class.getName()).log(Level.SEVERE, "Could not call method '" + method + "' of " + clazz + " for deserialization: method returned null");
            } else {
                return result;
            }
        } catch (Throwable ex) {
            Logger.getLogger(ConfigSerialization.class.getName()).log(
                    Level.SEVERE,
                    "Could not call method '" + method + "' of " + clazz + " for deserialization",
                    ex instanceof InvocationTargetException ? ex.getCause() : ex);
        }

        return null;
    }

    protected ConfigSerializable deserializeViaCtor(Constructor<? extends ConfigSerializable> ctor, Map<String, ?> args) {
        try {
            return ctor.newInstance(args);
        } catch (Throwable ex) {
            Logger.getLogger(ConfigSerialization.class.getName()).log(
                    Level.SEVERE,
                    "Could not call constructor '" + ctor + "' of " + clazz + " for deserialization",
                    ex instanceof InvocationTargetException ? ex.getCause() : ex);
        }

        return null;
    }

    public ConfigSerializable deserialize(Map<String, ?> args) {
        ConfigSerializable result = null;
        Method method;

        if (result == null) {
            method = getMethod("deserialize");

            if (method != null) {
                result = deserializeViaMethod(method, args);
            }
        }

        if (result == null) {
            method = getMethod("valueOf");

            if (method != null) {
                result = deserializeViaMethod(method, args);
            }
        }

        if (result == null) {
            Constructor<? extends ConfigSerializable> constructor = getConstructor();

            if (constructor != null) {
                result = deserializeViaCtor(constructor, args);
            }
        }

        return result;
    }
    
    public static ConfigSerializable deserializeObject(Map<String, ?> args, Class<? extends ConfigSerializable> clazz) {
        return new ConfigSerialization(clazz).deserialize(args);
    }
    
    public static ConfigSerializable deserializeObject(Map<String, ?> args) {
        Class<? extends ConfigSerializable> clazz;

        if (args.containsKey(SERIALIZED_TYPE_KEY)) {
            try {
                String alias = (String) args.get(SERIALIZED_TYPE_KEY);

                if (alias == null) {
                    throw new IllegalArgumentException("Cannot have null alias");
                }
                clazz = getClassByAlias(alias);
                if (clazz == null) {
                    throw new IllegalArgumentException("Specified class does not exist ('" + alias + "')");
                }
            } catch (ClassCastException ex) {
                ex.fillInStackTrace();
                throw ex;
            }
        } else {
            throw new IllegalArgumentException("Args doesn't contain type key ('" + SERIALIZED_TYPE_KEY + "')");
        }

        return new ConfigSerialization(clazz).deserialize(args);
    }
    
    public static void registerClass(Class<? extends ConfigSerializable> clazz) {
        DelegateDeserialization delegate = clazz.getAnnotation(DelegateDeserialization.class);

        if (delegate == null) {
            registerClass(clazz, getAlias(clazz));
            registerClass(clazz, clazz.getName());
        }
    }
    
    public static void registerClass(Class<? extends ConfigSerializable> clazz, String alias) {
        aliases.put(alias, clazz);
    }

    public static void unregisterClass(String alias) {
        aliases.remove(alias);
    }
    
    public static void unregisterClass(Class<? extends ConfigSerializable> clazz) {
        aliases.values().remove(clazz);
    }

    public static Class<? extends ConfigSerializable> getClassByAlias(String alias) {
        return aliases.get(alias);
    }
    
    public static String getAlias(Class<? extends ConfigSerializable> clazz) {
        DelegateDeserialization delegate = clazz.getAnnotation(DelegateDeserialization.class);

        if (delegate != null) {
            if ((delegate.value() == null) || (delegate.value() == clazz)) {
                delegate = null;
            } else {
                return getAlias(delegate.value());
            }
        }

        if (delegate == null) {
            SerializableAs alias = clazz.getAnnotation(SerializableAs.class);

            if ((alias != null) && (alias.value() != null)) {
                return alias.value();
            }
        }

        return clazz.getName();
    }
}
