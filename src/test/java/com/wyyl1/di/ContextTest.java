package com.wyyl1.di;

import com.google.common.annotations.VisibleForTesting;
import com.wyyl1.di.InjectionTest.ConstructorInjection.Injection.InjectConstructor;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.stream.Stream;

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

            Provider<Component> provider = context.get(new Context.Ref<Provider<Component>>() {}).get();
            assertSame(instance, provider.get());
        }

        @Test
        void should_not_retrieve_bind_type_as_unsupported_container() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            Context context = config.getContext();

            assertFalse(context.get(new Context.Ref<List<Component>>() {}).isPresent());
        }

        @Nested
        class WithQualifier{
            @Test
            void should_bind_instance_with_qualifier() {
                Component instance = new Component() {
                };
                config.bind(Component.class, instance, new NamedLiteral("ChosenOne"));

                Context context = config.getContext();

                Component chosenOne = context.get(Context.Ref.of(Component.class, new NamedLiteral("ChosenOne"))).get();
                assertSame(instance, chosenOne);
            }

            @Test
            void should_bind_component_with_qualifier() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(InjectConstructor.class,
                        InjectConstructor.class, new NamedLiteral("ChosenOne"));

                Context context = config.getContext();

                InjectConstructor chosenOne = context.get(Context.Ref.of(InjectConstructor.class, new NamedLiteral("ChosenOne"))).get();
                assertSame(dependency, chosenOne.dependency);
            }

            @Test
            void should_bind_instance_with_multi_qualifier() {
                Component instance = new Component() {
                };
                config.bind(Component.class, instance, new NamedLiteral("ChosenOne"), new NamedLiteral("Skywalker"));

                Context context = config.getContext();
                Component chosenOne = context.get(Context.Ref.of(Component.class, new NamedLiteral("ChosenOne"))).get();
                Component skywalker = context.get(Context.Ref.of(Component.class, new NamedLiteral("Skywalker"))).get();

                assertSame(instance, chosenOne);
                assertSame(instance, skywalker);
            }

            @Test
            void should_bind_component_with_multi_qualifier() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(InjectConstructor.class,
                        InjectConstructor.class, new NamedLiteral("ChosenOne"),
                        new NamedLiteral("Skywalker"));

                Context context = config.getContext();

                InjectConstructor chosenOne = context.get(Context.Ref.of(InjectConstructor.class, new NamedLiteral("ChosenOne"))).get();
                InjectConstructor skywalker = context.get(Context.Ref.of(InjectConstructor.class, new NamedLiteral("Skywalker"))).get();
                assertSame(dependency, chosenOne.dependency);
                assertSame(dependency, skywalker.dependency);
            }
        }
    }

    @Nested
    class DependencyCheck {

        @ParameterizedTest
        @MethodSource("should_throw_exception_if_dependency_not_found")
        void should_throw_exception_if_dependency_not_found(Class<? extends Component> component) {
            config.bind(Component.class, component);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());
        }

        static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(Arguments.of(Named.of("Inject Constructor", DependencyCheck.MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Inject Field", DependencyCheck.MissingDependencyField.class)),
                    Arguments.of(Named.of("Inject Method", DependencyCheck.MissingDependencyMethod.class)),
                    Arguments.of(Named.of("Provider in Inject Constructor", DependencyCheck.MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider in Inject Field", DependencyCheck.MissingDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider in Inject Method", DependencyCheck.MissingDependencyProviderMethod.class))
            );
        }

        static class MissingDependencyConstructor implements Component {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyProviderField implements Component {
            @Inject
            Provider<Dependency> dependency;
        }

        static class MissingDependencyProviderMethod implements Component {
            @Inject
            void install(Provider<Dependency> dependency){}
        }

        static class MissingDependencyField implements Component {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyMethod implements Component {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class MissingDependencyProviderConstructor implements Component {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<Component> component) {
            }
        }

        static class CyclicComponentInjectConstructor implements Component{
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        @Test
        void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(Component.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);

            Context context = config.getContext();
            assertTrue(context.get(Context.Ref.of(Component.class)).isPresent());
        }
    }
}

record NamedLiteral(String value) implements jakarta.inject.Named {

    @Override
    public Class<? extends Annotation> annotationType() {
        return jakarta.inject.Named.class;
    }
}
