package com.stardevllc.config;

import java.util.Map;

public interface Config extends Section {
    @Override
    void addDefault(String path, Object value);
    
    void addDefaults(Map<String, Object> defaults);
    
    void addDefaults(Config defaults);
    
    void setDefaults(Config defaults);
    
    Config getDefaults();
    
    Options options();

    class Options {
        private char pathSeparator = '.';
        private boolean copyDefaults = false;
        private final Config configuration;
    
        protected Options(Config configuration) {
            this.configuration = configuration;
        }
        
        public Config configuration() {
            return configuration;
        }
        
        public char pathSeparator() {
            return pathSeparator;
        }
        
        public Options pathSeparator(char value) {
            this.pathSeparator = value;
            return this;
        }
        
        public boolean copyDefaults() {
            return copyDefaults;
        }
        
        public Options copyDefaults(boolean value) {
            this.copyDefaults = value;
            return this;
        }
    }
}
