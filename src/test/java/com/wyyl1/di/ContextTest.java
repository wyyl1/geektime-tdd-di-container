package com.wyyl1.di;

import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author aoe
 * @date 2022/5/6
 */
@Nested
class ContextTest {

    private ContextConfig config;

    @BeforeEach
    void setup() {
        config = new ContextConfig();
    }

    @Nested
    class TypeBinding {
        @Test
        void should_retrieve_bind_type_as_provider() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            Context context = config.getContext();

            ParameterizedType type = new TypeLiteral<Provider<Component>>() {
            }.getType();

            Provider<Component> provider = (Provider<Component>)context.get(type).get();
            assertSame(instance, provider.get());
        }

        @Test
        void should_not_retrieve_bind_type_as_unsupported_container() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            Context context = config.getContext();

            ParameterizedType type = new TypeLiteral<List<Component>>() {
            }.getType();

            assertFalse(context.get(type).isPresent());
        }

        static abstract class TypeLiteral<T> {
            public ParameterizedType getType() {
                return (ParameterizedType)((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            }
        }
    }

}
