package me.hsgamer.bettergui.betterforms.component.impl;

import me.hsgamer.bettergui.betterforms.builder.ComponentBuilder;
import me.hsgamer.bettergui.betterforms.component.Component;
import me.hsgamer.bettergui.betterforms.component.FormResponseHandler;
import me.hsgamer.bettergui.betterforms.util.ComponentUtil;
import me.hsgamer.hscore.collections.map.CaseInsensitiveStringMap;
import me.hsgamer.hscore.common.MapUtils;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HybridComponent extends Component {
    private final Map<String, Component> components;

    public HybridComponent(ComponentBuilder.Input input) {
        super(input);
        this.components = Optional.ofNullable(MapUtils.getIfFound(input.options, "child"))
                .flatMap(MapUtils::castOptionalStringObjectMap)
                .orElseGet(Collections::emptyMap)
                .entrySet()
                .stream()
                .flatMap(entry -> {
                    String key = entry.getKey();
                    return MapUtils.castOptionalStringObjectMap(entry.getValue())
                            .map(CaseInsensitiveStringMap::new)
                            .map(map -> new ComponentBuilder.Input(input.menu, input.name + "_child_" + key, map))
                            .flatMap(ComponentBuilder.INSTANCE::build)
                            .map(component -> new AbstractMap.SimpleImmutableEntry<>(key, component))
                            .map(Stream::of)
                            .orElseGet(Stream::empty);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    @Override
    public @Nullable List<FormResponseHandler> apply(UUID uuid, int index, FormBuilder<?, ?, ?> builder) {
        List<FormResponseHandler> finalHandlers = new ArrayList<>();
        for (Component component : components.values()) {
            List<FormResponseHandler> handlers = component.apply(uuid, index + finalHandlers.size(), builder);
            if (handlers != null) {
                finalHandlers.addAll(handlers);
            }
        }
        return finalHandlers.isEmpty() ? null : finalHandlers;
    }

    @Override
    public String getValue(UUID uuid, String args) {
        ComponentUtil.ComponentKey componentKey = ComponentUtil.toComponentKey(args);
        return Optional.ofNullable(components.get(componentKey.key))
                .map(component -> component.getValue(uuid, componentKey.arguments))
                .orElse("");
    }
}
