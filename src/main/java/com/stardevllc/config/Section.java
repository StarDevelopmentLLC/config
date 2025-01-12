package com.stardevllc.config;

import com.stardevllc.config.serialization.ConfigSerializable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Section {
    Set<String> getKeys(boolean deep);
    
    default Set<String> getKeys() {
        return getKeys(false);
    }

    Map<String, Object> getValues(boolean deep);
    
    default Map<String, Object> getValues() {
        return getValues(false);
    }

    boolean contains(String path);

    boolean contains(String path, boolean ignoreDefault);

    boolean isSet(String path);

    String getCurrentPath();

    String getName();

    Config getRoot();

    Section getParent();

    Object get(String path);

    Object get(String path, Object def);

    void set(String path, Object value);

    Section createSection(String path);

    Section createSection(String path, Map<?, ?> map);

    String getString(String path);

    String getString(String path, String def);

    boolean isString(String path);

    int getInt(String path);

    int getInt(String path, int def);

    boolean isInt(String path);

    boolean getBoolean(String path);

    boolean getBoolean(String path, boolean def);

    boolean isBoolean(String path);

    double getDouble(String path);

    double getDouble(String path, double def);

    boolean isDouble(String path);

    long getLong(String path);

    long getLong(String path, long def);

    boolean isLong(String path);

    List<?> getList(String path);

    List<?> getList(String path, List<?> def);

    boolean isList(String path);

    List<String> getStringList(String path);

    List<Integer> getIntegerList(String path);

    List<Boolean> getBooleanList(String path);

    List<Double> getDoubleList(String path);

    List<Float> getFloatList(String path);

    List<Long> getLongList(String path);

    List<Byte> getByteList(String path);

    List<Character> getCharacterList(String path);

    List<Short> getShortList(String path);

    List<Map<?, ?>> getMapList(String path);

    <T> T getObject(String path, Class<T> clazz);

    <T> T getObject(String path, Class<T> clazz, T def);

    <T extends ConfigSerializable> T getSerializable(String path, Class<T> clazz);

    <T extends ConfigSerializable> T getSerializable(String path, Class<T> clazz, T def);

    Section getConfigurationSection(String path);
    
    default Section getSection(String path) {
        return getConfigurationSection(path);
    }

    boolean isConfigurationSection(String path);
    
    default boolean isSection(String path) {
        return isConfigurationSection(path);
    }

    Section getDefaultSection();

    void addDefault(String path, Object value);

    List<String> getComments(String path);

    List<String> getInlineComments(String path);

    void setComments(String path, List<String> comments);

    void setComments(String path, String... comments);

    void setInlineComments(String path, List<String> comments);

    void setInlineComments(String path, String... comments);
}
