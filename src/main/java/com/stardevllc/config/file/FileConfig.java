package com.stardevllc.config.file;

import com.stardevllc.config.Config;
import com.stardevllc.config.InvalidConfigException;
import com.stardevllc.config.MemoryConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public abstract class FileConfig extends MemoryConfig {
    
    protected File file;
    
    public FileConfig(File file) {
        super();
        this.file = file;
    }
    
    public FileConfig(File file, Config defaults) {
        super(defaults);
        this.file = file;
    }
    
    public void reload(boolean save) {
        if (save) {
            save();
        }
        
        this.load();
    }
    
    public void delete() {
        try {
            Files.delete(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void save() {
        try {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected abstract String saveToString();
    
    public void load() {
        try {
            if (!Files.exists(file.toPath().getParent())) {
                Files.createDirectories(file.toPath().getParent());
            }
            
            if (!file.exists()) {
                file.createNewFile();
            }
            
            final FileInputStream stream = new FileInputStream(file);
            
            load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected void load(Reader reader) throws IOException, InvalidConfigException {
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
    
    protected abstract void loadFromString(String contents) throws InvalidConfigException;
    
    @Override
    public Options options() {
        if (options == null) {
            options = new Options(this);
        }
        
        return (Options) options;
    }
    
    public void renameFile(String newName) {
        Path parent = file.toPath().toAbsolutePath().getParent();
        Path newPath = FileSystems.getDefault().getPath(parent.toString(), newName);
        try {
            Files.move(file.toPath(), newPath, REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
