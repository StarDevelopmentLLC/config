package com.stardevllc.config.file.yaml;

import com.stardevllc.config.Section;
import com.stardevllc.config.InvalidConfigException;
import com.stardevllc.config.file.FileConfig;
import com.stardevllc.config.serialization.ConfigSerialization;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class YamlConfig extends FileConfig {
    private final DumperOptions yamlDumperOptions;
    private final LoaderOptions yamlLoaderOptions;
    private final YamlConstructor constructor;
    private final YamlRepresenter representer;
    private final Yaml yaml;

    public YamlConfig() {
        yamlDumperOptions = new DumperOptions();
        yamlDumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yamlLoaderOptions = new LoaderOptions();
        yamlLoaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
        yamlLoaderOptions.setCodePointLimit(Integer.MAX_VALUE); 
        yamlLoaderOptions.setNestingDepthLimit(100);

        constructor = new YamlConstructor(yamlLoaderOptions);
        representer = new YamlRepresenter(yamlDumperOptions);
        representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        yaml = new Yaml(constructor, representer, yamlDumperOptions, yamlLoaderOptions);
    }

    @Override
    public String saveToString() {
        yamlDumperOptions.setIndent(options().indent());
        yamlDumperOptions.setWidth(options().width());
        yamlDumperOptions.setProcessComments(options().parseComments());

        MappingNode node = toNodeTree(this);

        node.setBlockComments(getCommentLines(saveHeader(options().getHeader()), CommentType.BLOCK));
        node.setEndComments(getCommentLines(options().getFooter(), CommentType.BLOCK));

        StringWriter writer = new StringWriter();
        if (node.getBlockComments().isEmpty() && node.getEndComments().isEmpty() && node.getValue().isEmpty()) {
            writer.write("");
        } else {
            if (node.getValue().isEmpty()) {
                node.setFlowStyle(DumperOptions.FlowStyle.FLOW);
            }
            yaml.serialize(node, writer);
        }
        return writer.toString();
    }

    @Override
    public void loadFromString(String contents) throws InvalidConfigException {
        yamlLoaderOptions.setProcessComments(options().parseComments());

        MappingNode node;
        try (Reader reader = new UnicodeReader(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)))) {
            Node rawNode = yaml.compose(reader);
            try {
                node = (MappingNode) rawNode;
            } catch (ClassCastException e) {
                throw new InvalidConfigException("Top level is not a Map.");
            }
        } catch (YAMLException | IOException | ClassCastException e) {
            throw new InvalidConfigException(e);
        }

        this.map.clear();

        if (node != null) {
            adjustNodeComments(node);
            options().setHeader(loadHeader(getCommentLines(node.getBlockComments())));
            options().setFooter(getCommentLines(node.getEndComments()));
            fromNodeTree(node, this);
        }
    }
    
    private void adjustNodeComments(final MappingNode node) {
        if (node.getBlockComments() == null && !node.getValue().isEmpty()) {
            Node firstNode = node.getValue().getFirst().getKeyNode();
            List<CommentLine> lines = firstNode.getBlockComments();
            if (lines != null) {
                int index = -1;
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).getCommentType() == CommentType.BLANK_LINE) {
                        index = i;
                    }
                }
                if (index != -1) {
                    node.setBlockComments(lines.subList(0, index + 1));
                    firstNode.setBlockComments(lines.subList(index + 1, lines.size()));
                }
            }
        }
    }

    private void fromNodeTree(MappingNode input, Section section) {
        constructor.flattenMapping(input);
        for (NodeTuple nodeTuple : input.getValue()) {
            Node key = nodeTuple.getKeyNode();
            String keyString = String.valueOf(constructor.construct(key));
            Node value = nodeTuple.getValueNode();

            while (value instanceof AnchorNode) {
                value = ((AnchorNode) value).getRealNode();
            }

            if (value instanceof MappingNode && !hasSerializedTypeKey((MappingNode) value)) {
                fromNodeTree((MappingNode) value, section.createSection(keyString));
            } else {
                section.set(keyString, constructor.construct(value));
            }

            section.setComments(keyString, getCommentLines(key.getBlockComments()));
            if (value instanceof MappingNode || value instanceof SequenceNode) {
                section.setInlineComments(keyString, getCommentLines(key.getInLineComments()));
            } else {
                section.setInlineComments(keyString, getCommentLines(value.getInLineComments()));
            }
        }
    }

    private boolean hasSerializedTypeKey(MappingNode node) {
        for (NodeTuple nodeTuple : node.getValue()) {
            Node keyNode = nodeTuple.getKeyNode();
            if (!(keyNode instanceof ScalarNode)) {
                continue;
            }
            String key = ((ScalarNode) keyNode).getValue();
            if (key.equals(ConfigSerialization.SERIALIZED_TYPE_KEY)) {
                return true;
            }
        }
        return false;
    }

    private MappingNode toNodeTree(Section section) {
        List<NodeTuple> nodeTuples = new ArrayList<>();
        for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
            Node key = representer.represent(entry.getKey());
            Node value;
            if (entry.getValue() instanceof Section) {
                value = toNodeTree((Section) entry.getValue());
            } else {
                value = representer.represent(entry.getValue());
            }
            key.setBlockComments(getCommentLines(section.getComments(entry.getKey()), CommentType.BLOCK));
            if (value instanceof MappingNode || value instanceof SequenceNode) {
                key.setInLineComments(getCommentLines(section.getInlineComments(entry.getKey()), CommentType.IN_LINE));
            } else {
                value.setInLineComments(getCommentLines(section.getInlineComments(entry.getKey()), CommentType.IN_LINE));
            }

            nodeTuples.add(new NodeTuple(key, value));
        }

        return new MappingNode(Tag.MAP, nodeTuples, DumperOptions.FlowStyle.BLOCK);
    }

    private List<String> getCommentLines(List<CommentLine> comments) {
        List<String> lines = new ArrayList<>();
        if (comments != null) {
            for (CommentLine comment : comments) {
                if (comment.getCommentType() == CommentType.BLANK_LINE) {
                    lines.add(null);
                } else {
                    String line = comment.getValue();
                    line = line.startsWith(" ") ? line.substring(1) : line;
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    private List<CommentLine> getCommentLines(List<String> comments, CommentType commentType) {
        List<CommentLine> lines = new ArrayList<>();
        for (String comment : comments) {
            if (comment == null) {
                lines.add(new CommentLine(null, null, "", CommentType.BLANK_LINE));
            } else {
                String line = comment;
                line = line.isEmpty() ? line : " " + line;
                lines.add(new CommentLine(null, null, line, commentType));
            }
        }
        return lines;
    }
    
    private List<String> loadHeader(List<String> header) {
        LinkedList<String> list = new LinkedList<>(header);

        if (!list.isEmpty()) {
            list.removeLast();
        }

        while (!list.isEmpty() && list.peek() == null) {
            list.remove();
        }

        return list;
    }
    
    private List<String> saveHeader(List<String> header) {
        LinkedList<String> list = new LinkedList<>(header);

        if (!list.isEmpty()) {
            list.add(null);
        }

        return list;
    }

    @Override
    public Options options() {
        if (options == null) {
            options = new Options(this);
        }

        return (Options) options;
    }
    
    public static YamlConfig loadConfiguration(File file) {
        YamlConfig config = new YamlConfig();

        try {
            config.load(file);
        } catch (Exception ex) {
            
        }

        return config;
    }
    
    public static YamlConfig loadConfiguration(Reader reader) {
        YamlConfig config = new YamlConfig();

        try {
            config.load(reader);
        } catch (Exception ex) {
            
        } 

        return config;
    }

    public static class Options extends FileConfig.Options {
        private int indent = 2;
        private int width = 80;
    
        protected Options(YamlConfig configuration) {
            super(configuration);
        }
    
        @Override
        public YamlConfig configuration() {
            return (YamlConfig) super.configuration();
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
    
        @Override
        public Options setHeader(List<String> value) {
            super.setHeader(value);
            return this;
        }
    
        @Override
        public Options setFooter(List<String> value) {
            super.setFooter(value);
            return this;
        }
    
        @Override
        public Options parseComments(boolean value) {
            super.parseComments(value);
            return this;
        }
        
        public int indent() {
            return indent;
        }
        
        public Options indent(int value) {
            this.indent = value;
            return this;
        }
        
        public int width() {
            return width;
        }
        
        public Options width(int value) {
            this.width = value;
            return this;
        }
    }
}
