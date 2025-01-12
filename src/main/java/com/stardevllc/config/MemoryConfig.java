package com.stardevllc.config;

import java.util.Map;

public class MemoryConfig extends MemorySection implements Config {
    protected Config defaults;
    protected Options options;

    public MemoryConfig() {
    }

    public MemoryConfig(Config defaults) {
        this.defaults = defaults;
    }

    @Override
    public void addDefault(String path, Object value) {
        if (defaults == null) {
            defaults = new MemoryConfig();
        }

        defaults.set(path, value);
    }

    @Override
    public void addDefaults(Map<String, Object> defaults) {
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            addDefault(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void addDefaults(Config defaults) {
        for (String key : defaults.getKeys(true)) {
            if (!defaults.isConfigurationSection(key)) {
                addDefault(key, defaults.get(key));
            }
        }
    }

    @Override
    public void setDefaults(Config defaults) {
        this.defaults = defaults;
    }

    @Override
    public Config getDefaults() {
        return defaults;
    }

    @Override
    public Section getParent() {
        return null;
    }

    @Override
    public Options options() {
        if (options == null) {
            options = new Options(this);
        }

        return options;
    }

    public static class Options extends Config.Options {
        protected Options(MemoryConfig configuration) {
            super(configuration);
        }

        @Override
        public MemoryConfig configuration() {
            return (MemoryConfig) super.configuration();
        }

        @Override
        public Options copyDefaults(boolean value) {
            super.copyDefaults(value);
            return this;
        }

        @Override
        public Options pathSeparator(char value) {
            super.pathSeparator(value);
            return this;
        }
    }
}
