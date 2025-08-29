package com.stardevllc.config.file;

import com.stardevllc.config.Config;
import com.stardevllc.config.InvalidConfigException;
import com.stardevllc.config.MemoryConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public abstract class FileConfig extends MemoryConfig {
    
    public FileConfig() {
        super();
    }
    
    public FileConfig(Config defaults) {
        super(defaults);
    }
    
    public void save(File file) throws IOException {
        if (!Files.exists(file.toPath().getParent())) {
            Files.createDirectories(file.toPath().getParent());
        }
        
        if (!file.exists()) {
            file.createNewFile();
        }

        String data = saveToString();

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(data);
        }
    }
    
    public void save(String file) throws IOException {
        save(new File(file));
    }
    
    public abstract String saveToString();
    
    public void load(File file) throws IOException, InvalidConfigException {
        final FileInputStream stream = new FileInputStream(file);

        load(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
    
    public void load(Reader reader) throws IOException, InvalidConfigException {
        BufferedReader input = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);

        StringBuilder builder = new StringBuilder();

        try {
            String line;

            while ((line = input.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }
        } finally {
            input.close();
        }

        loadFromString(builder.toString());
    }
    
    public void load(String file) throws IOException, InvalidConfigException {
        load(new File(file));
    }
    
    public abstract void loadFromString(String contents) throws InvalidConfigException;
    
    @Override
    public Options options() {
        if (options == null) {
            options = new Options(this);
        }

        return (Options) options;
    }

    public static class Options extends MemoryConfig.Options {
        private List<String> header = Collections.emptyList();
        private List<String> footer = Collections.emptyList();
        private boolean parseComments = true;
    
        protected Options(MemoryConfig configuration) {
            super(configuration);
        }
    
        @Override
        public FileConfig configuration() {
            return (FileConfig) super.configuration();
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
        
        public List<String> getHeader() {
            return header;
        }
        
        public Options setHeader(List<String> value) {
            this.header = value == null ? Collections.emptyList() : Collections.unmodifiableList(value);
            return this;
        }
        
        public List<String> getFooter() {
            return footer;
        }
        
        public Options setFooter(List<String> value) {
            this.footer = value == null ? Collections.emptyList() : Collections.unmodifiableList(value);
            return this;
        }
        
        public boolean parseComments() {
            return parseComments;
        }
        
        public Options parseComments(boolean value) {
            parseComments = value;
            return this;
        }
    }
}
