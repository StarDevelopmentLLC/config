package com.stardevllc.config;

import com.stardevllc.config.serialization.ConfigSerializable;

import java.util.*;

import static com.stardevllc.config.NumberConversions.*;

public class MemorySection implements Section {
    protected final Map<String, SectionPathData> map = new LinkedHashMap<>();
    private final Config root;
    private final Section parent;
    private final String path;
    private final String fullPath;

    protected MemorySection() {
        if (!(this instanceof Config)) {
            throw new IllegalStateException("Cannot construct a root MemorySection when not a Configuration");
        }

        this.path = "";
        this.fullPath = "";
        this.parent = null;
        this.root = (Config) this;
    }

    protected MemorySection(Section parent, String path) {
        this.path = path;
        this.parent = parent;
        this.root = parent.getRoot();

        this.fullPath = createPath(parent, path);
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        Set<String> result = new LinkedHashSet<>();

        Config root = getRoot();
        if (root != null && root.options().copyDefaults()) {
            Section defaults = getDefaultSection();

            if (defaults != null) {
                result.addAll(defaults.getKeys(deep));
            }
        }

        mapChildrenKeys(result, this, deep);

        return result;
    }

    @Override
    public Map<String, Object> getValues(boolean deep) {
        Map<String, Object> result = new LinkedHashMap<>();

        Config root = getRoot();
        if (root != null && root.options().copyDefaults()) {
            Section defaults = getDefaultSection();

            if (defaults != null) {
                result.putAll(defaults.getValues(deep));
            }
        }

        mapChildrenValues(result, this, deep);

        return result;
    }

    @Override
    public boolean contains(String path) {
        return contains(path, false);
    }

    @Override
    public boolean contains(String path, boolean ignoreDefault) {
        return (ignoreDefault ? get(path, null) : get(path)) != null;
    }

    @Override
    public boolean isSet(String path) {
        Config root = getRoot();
        if (root == null) {
            return false;
        }
        if (root.options().copyDefaults()) {
            return contains(path);
        }
        return get(path, null) != null;
    }

    @Override
    public String getCurrentPath() {
        return fullPath;
    }

    @Override
    public String getName() {
        return path;
    }

    @Override
    public Config getRoot() {
        return root;
    }

    @Override
    public Section getParent() {
        return parent;
    }

    @Override
    public void addDefault(String path, Object value) {
        Config root = getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot add default without root");
        }
        if (root == this) {
            throw new UnsupportedOperationException("Unsupported addDefault(String, Object) implementation");
        }
        root.addDefault(createPath(this, path), value);
    }

    @Override
    public Section getDefaultSection() {
        Config root = getRoot();
        Config defaults = root == null ? null : root.getDefaults();

        if (defaults != null) {
            if (defaults.isConfigurationSection(getCurrentPath())) {
                return defaults.getConfigurationSection(getCurrentPath());
            }
        }

        return null;
    }

    @Override
    public void set(String path, Object value) {
        Config root = getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot use section without a root");
        }

        final char separator = root.options().pathSeparator();
        // i1 is the leading (higher) index
        // i2 is the trailing (lower) index
        int i1 = -1, i2;
        Section section = this;
        while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            String node = path.substring(i2, i1);
            Section subSection = section.getConfigurationSection(node);
            if (subSection == null) {
                if (value == null) {
                    // no need to create missing sub-sections if we want to remove the value:
                    return;
                }
                section = section.createSection(node);
            } else {
                section = subSection;
            }
        }

        String key = path.substring(i2);
        if (section == this) {
            if (value == null) {
                map.remove(key);
            } else {
                SectionPathData entry = map.get(key);
                if (entry == null) {
                    map.put(key, new SectionPathData(value));
                } else {
                    entry.setData(value);
                }
            }
        } else {
            section.set(key, value);
        }
    }

    @Override
    public Object get(String path) {
        return get(path, getDefault(path));
    }

    @Override
    public Object get(String path, Object def) {
        if (path.isEmpty()) {
            return this;
        }

        Config root = getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot access section without a root");
        }

        final char separator = root.options().pathSeparator();
        // i1 is the leading (higher) index
        // i2 is the trailing (lower) index
        int i1 = -1, i2;
        Section section = this;
        while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            final String currentPath = path.substring(i2, i1);
            if (!section.contains(currentPath, true)) {
                return def;
            }
            section = section.getConfigurationSection(currentPath);
            if (section == null) {
                return def;
            }
        }

        String key = path.substring(i2);
        if (section == this) {
            SectionPathData result = map.get(key);
            return result == null ? def : result.getData();
        }
        return section.get(key, def);
    }

    @Override
    public Section createSection(String path) {
        Config root = getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot create section without a root");
        }

        final char separator = root.options().pathSeparator();
        // i1 is the leading (higher) index
        // i2 is the trailing (lower) index
        int i1 = -1, i2;
        Section section = this;
        while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            String node = path.substring(i2, i1);
            Section subSection = section.getConfigurationSection(node);
            if (subSection == null) {
                section = section.createSection(node);
            } else {
                section = subSection;
            }
        }

        String key = path.substring(i2);
        if (section == this) {
            Section result = new MemorySection(this, key);
            map.put(key, new SectionPathData(result));
            return result;
        }
        return section.createSection(key);
    }

    @Override
    public Section createSection(String path, Map<?, ?> map) {
        Section section = createSection(path);

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                section.createSection(entry.getKey().toString(), (Map<?, ?>) entry.getValue());
            } else {
                section.set(entry.getKey().toString(), entry.getValue());
            }
        }

        return section;
    }

    // Primitives
    @Override
    public String getString(String path) {
        Object def = getDefault(path);
        return getString(path, def != null ? def.toString() : null);
    }

    @Override
    public String getString(String path, String def) {
        Object val = get(path, def);
        return val != null ? val.toString() : def;
    }

    @Override
    public boolean isString(String path) {
        Object val = get(path);
        return val instanceof String;
    }

    @Override
    public int getInt(String path) {
        Object def = getDefault(path);
        return getInt(path, def instanceof Number ? toInt(def) : 0);
    }

    @Override
    public int getInt(String path, int def) {
        Object val = get(path, def);
        return val instanceof Number ? toInt(val) : def;
    }

    @Override
    public boolean isInt(String path) {
        Object val = get(path);
        return val instanceof Integer;
    }

    @Override
    public boolean getBoolean(String path) {
        Object def = getDefault(path);
        return getBoolean(path, def instanceof Boolean ? (Boolean) def : false);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        Object val = get(path, def);
        return val instanceof Boolean ? (Boolean) val : def;
    }

    @Override
    public boolean isBoolean(String path) {
        Object val = get(path);
        return val instanceof Boolean;
    }

    @Override
    public double getDouble(String path) {
        Object def = getDefault(path);
        return getDouble(path, def instanceof Number ? toDouble(def) : 0);
    }

    @Override
    public double getDouble(String path, double def) {
        Object val = get(path, def);
        return val instanceof Number ? toDouble(val) : def;
    }

    @Override
    public boolean isDouble(String path) {
        Object val = get(path);
        return val instanceof Double;
    }

    @Override
    public long getLong(String path) {
        Object def = getDefault(path);
        return getLong(path, def instanceof Number ? toLong(def) : 0);
    }

    @Override
    public long getLong(String path, long def) {
        Object val = get(path, def);
        return val instanceof Number ? toLong(val) : def;
    }

    @Override
    public boolean isLong(String path) {
        Object val = get(path);
        return val instanceof Long;
    }

    // Java
    @Override
    public List<?> getList(String path) {
        Object def = getDefault(path);
        return getList(path, def instanceof List ? (List<?>) def : null);
    }

    @Override
    public List<?> getList(String path, List<?> def) {
        Object val = get(path, def);
        return (List<?>) (val instanceof List ? val : def);
    }

    @Override
    public boolean isList(String path) {
        Object val = get(path);
        return val instanceof List;
    }

    @Override
    public List<String> getStringList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<String> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof String || isPrimitiveWrapper(object)) {
                result.add(String.valueOf(object));
            }
        }

        return result;
    }

    @Override
    public List<Integer> getIntegerList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Integer> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Integer) {
                result.add((Integer) object);
            } else if (object instanceof String) {
                try {
                    result.add(Integer.valueOf((String) object));
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((int) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).intValue());
            }
        }

        return result;
    }

    @Override
    public List<Boolean> getBooleanList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Boolean> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Boolean) {
                result.add((Boolean) object);
            } else if (object instanceof String) {
                if (Boolean.TRUE.toString().equals(object)) {
                    result.add(true);
                } else if (Boolean.FALSE.toString().equals(object)) {
                    result.add(false);
                }
            }
        }

        return result;
    }

    @Override
    public List<Double> getDoubleList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Double> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Double) {
                result.add((Double) object);
            } else if (object instanceof String) {
                try {
                    result.add(Double.valueOf((String) object));
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((double) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).doubleValue());
            }
        }

        return result;
    }

    @Override
    public List<Float> getFloatList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Float> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Float) {
                result.add((Float) object);
            } else if (object instanceof String) {
                try {
                    result.add(Float.valueOf((String) object));
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((float) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).floatValue());
            }
        }

        return result;
    }

    @Override
    public List<Long> getLongList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Long> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Long) {
                result.add((Long) object);
            } else if (object instanceof String) {
                try {
                    result.add(Long.valueOf((String) object));
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((long) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).longValue());
            }
        }

        return result;
    }

    @Override
    public List<Byte> getByteList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Byte> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Byte) {
                result.add((Byte) object);
            } else if (object instanceof String) {
                try {
                    result.add(Byte.valueOf((String) object));
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((byte) ((Character) object).charValue());
            } else if (object instanceof Number) {
                result.add(((Number) object).byteValue());
            }
        }

        return result;
    }

    @Override
    public List<Character> getCharacterList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Character> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Character) {
                result.add((Character) object);
            } else if (object instanceof String str) {
                if (str.length() == 1) {
                    result.add(str.charAt(0));
                }
            } else if (object instanceof Number) {
                result.add((char) ((Number) object).intValue());
            }
        }

        return result;
    }

    @Override
    public List<Short> getShortList(String path) {
        List<?> list = getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Short> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Short) {
                result.add((Short) object);
            } else if (object instanceof String) {
                try {
                    result.add(Short.valueOf((String) object));
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((short) ((Character) object).charValue());
            } else if (object instanceof Number) {
                result.add(((Number) object).shortValue());
            }
        }

        return result;
    }

    @Override
    public List<Map<?, ?>> getMapList(String path) {
        List<?> list = getList(path);
        List<Map<?, ?>> result = new ArrayList<>();

        if (list == null) {
            return result;
        }

        for (Object object : list) {
            if (object instanceof Map) {
                result.add((Map<?, ?>) object);
            }
        }

        return result;
    }

    @Override
    public <T> T getObject(String path, Class<T> clazz) {
        Object def = getDefault(path);
        return getObject(path, clazz, clazz.isInstance(def) ? clazz.cast(def) : null);
    }

    @Override
    public <T> T getObject(String path, Class<T> clazz, T def) {
        Object val = get(path, def);
        return clazz.isInstance(val) ? clazz.cast(val) : def;
    }

    @Override
    public <T extends ConfigSerializable> T getSerializable(String path, Class<T> clazz) {
        return getObject(path, clazz);
    }

    @Override
    public <T extends ConfigSerializable> T getSerializable(String path, Class<T> clazz, T def) {
        return getObject(path, clazz, def);
    }

    @Override
    public Section getConfigurationSection(String path) {
        Object val = get(path, null);
        if (val != null) {
            return val instanceof Section ? (Section) val : null;
        }

        val = get(path, getDefault(path));
        return val instanceof Section ? createSection(path) : null;
    }

    @Override
    public boolean isConfigurationSection(String path) {
        Object val = get(path);
        return val instanceof Section;
    }

    protected boolean isPrimitiveWrapper(Object input) {
        return input instanceof Integer || input instanceof Boolean
                || input instanceof Character || input instanceof Byte
                || input instanceof Short || input instanceof Double
                || input instanceof Long || input instanceof Float;
    }

    protected Object getDefault(String path) {
        Config root = getRoot();
        Config defaults = root == null ? null : root.getDefaults();
        return defaults == null ? null : defaults.get(createPath(this, path));
    }

    protected void mapChildrenKeys(Set<String> output, Section section, boolean deep) {
        if (section instanceof MemorySection sec) {

            for (Map.Entry<String, SectionPathData> entry : sec.map.entrySet()) {
                output.add(createPath(section, entry.getKey(), this));

                if (deep && entry.getValue().getData() instanceof Section subsection) {
                    mapChildrenKeys(output, subsection, true);
                }
            }
        } else {
            Set<String> keys = section.getKeys(deep);

            for (String key : keys) {
                output.add(createPath(section, key, this));
            }
        }
    }

    protected void mapChildrenValues(Map<String, Object> output, Section section, boolean deep) {
        if (section instanceof MemorySection sec) {

            for (Map.Entry<String, SectionPathData> entry : sec.map.entrySet()) {
                String childPath = createPath(section, entry.getKey(), this);
                output.remove(childPath);
                output.put(childPath, entry.getValue().getData());

                if (entry.getValue().getData() instanceof Section) {
                    if (deep) {
                        mapChildrenValues(output, (Section) entry.getValue().getData(), true);
                    }
                }
            }
        } else {
            Map<String, Object> values = section.getValues(deep);

            for (Map.Entry<String, Object> entry : values.entrySet()) {
                output.put(createPath(section, entry.getKey(), this), entry.getValue());
            }
        }
    }

    @SuppressWarnings("ConstantValue")
    public static String createPath(Section section, String key) {
        return createPath(section, key, section == null ? null : section.getRoot());
    }

    public static String createPath(Section section, String key, Section relativeTo) {
        Config root = section.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot create path without a root");
        }
        char separator = root.options().pathSeparator();

        StringBuilder builder = new StringBuilder();
        for (Section parent = section; parent != null && parent != relativeTo; parent = parent.getParent()) {
            if (!builder.isEmpty()) {
                builder.insert(0, separator);
            }
            builder.insert(0, parent.getName());
        }

        if (key != null && !key.isEmpty()) {
            if (!builder.isEmpty()) {
                builder.append(separator);
            }

            builder.append(key);
        }

        return builder.toString();
    }

    @Override
    public List<String> getComments(final String path) {
        final SectionPathData pathData = getSectionPathData(path);
        return pathData == null ? Collections.emptyList() : pathData.getComments();
    }

    @Override
    public List<String> getInlineComments(final String path) {
        final SectionPathData pathData = getSectionPathData(path);
        return pathData == null ? Collections.emptyList() : pathData.getInlineComments();
    }

    @Override
    public void setComments(final String path, final List<String> comments) {
        final SectionPathData pathData = getSectionPathData(path);
        if (pathData != null) {
            pathData.setComments(comments);
        }
    }

    @Override
    public void setComments(final String path, final String... comments) {
        setComments(path, List.of(comments));
    }

    @Override
    public void setInlineComments(final String path, final List<String> comments) {
        final SectionPathData pathData = getSectionPathData(path);
        if (pathData != null) {
            pathData.setInlineComments(comments);
        }
    }

    @Override
    public void setInlineComments(final String path, final String... comments) {
        setComments(path, List.of(comments));
    }

    private SectionPathData getSectionPathData(String path) {
        Config root = getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot access section without a root");
        }

        final char separator = root.options().pathSeparator();
        // i1 is the leading (higher) index
        // i2 is the trailing (lower) index
        int i1 = -1, i2;
        Section section = this;
        while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            section = section.getConfigurationSection(path.substring(i2, i1));
            if (section == null) {
                return null;
            }
        }

        String key = path.substring(i2);
        if (section == this) {
            return map.get(key);
        } else if (section instanceof MemorySection) {
            return ((MemorySection) section).getSectionPathData(key);
        }
        return null;
    }

    @Override
    public String toString() {
        Config root = getRoot();
        return getClass().getSimpleName() +
                "[path='" +
                getCurrentPath() +
                "', root='" +
                (root == null ? null : root.getClass().getSimpleName()) +
                "']";
    }
}
