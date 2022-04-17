package com.wyyl1.di;

import jakarta.inject.Provider;

import java.util.HashMap;
import java.util.Map;

public class Context {

    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> type, ComponentType instance) {
        providers.put(type, () -> instance);
    }

    public <ComponentType, ComponentImplementation extends ComponentType>
    void bind(Class<ComponentType> type, Class<ComponentImplementation> implementation) {
        providers.put(type, () -> {
            try {
                return implementation.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <ComponentType> ComponentType get(Class<ComponentType> type) {
        return (ComponentType) providers.get(type).get();
    }
}
