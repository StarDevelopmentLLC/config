package com.stardevllc.config.file.yaml;

import java.util.LinkedHashMap;
import java.util.Map;

import com.stardevllc.config.Section;
import com.stardevllc.config.serialization.ConfigSerializable;
import com.stardevllc.config.serialization.ConfigSerialization;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Representer;

public class YamlRepresenter extends Representer {

    public YamlRepresenter() {
        this(new DumperOptions());
    }

    public YamlRepresenter(DumperOptions options) {
        super(options);
        this.multiRepresenters.put(Section.class, new RepresentConfigurationSection());
        this.multiRepresenters.put(ConfigSerializable.class, new RepresentConfigurationSerializable());
        this.multiRepresenters.remove(Enum.class);
    }

    private class RepresentConfigurationSection extends RepresentMap {
        @Override
        public Node representData(Object data) {
            return super.representData(((Section) data).getValues(false));
        }
    }

    private class RepresentConfigurationSerializable extends RepresentMap {
        @Override
        public Node representData(Object data) {
            ConfigSerializable serializable = (ConfigSerializable) data;
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(ConfigSerialization.SERIALIZED_TYPE_KEY, ConfigSerialization.getAlias(serializable.getClass()));
            values.putAll(serializable.serialize());

            return super.representData(values);
        }
    }
}
