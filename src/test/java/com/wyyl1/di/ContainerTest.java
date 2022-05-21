package com.wyyl1.di;

import com.google.common.collect.Sets;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    private ContextConfig config;

    @BeforeEach
    void setup() {
        config = new ContextConfig();
    }

    @Nested
    class ComponentConstruction {
        @Test
        void should_bind_type_to_a_specific_instance() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);

            assertSame(instance, config.getContext().get(ComponentRef.of(TestComponent.class)).get());
        }

        //todo abstract class
        //todo interface

        @Test
        void should_return_empty_if_component_not_defined() {
            Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class));
            assertTrue(component.isEmpty());
        }

        @Nested
        class DependencyCheck {
            @Test
            void should_throw_exception_if_dependency_not_found() {
                config.bind(TestComponent.class, ComponentWithInjectConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(TestComponent.class, exception.getComponent());
            }

            @Test
            void should_throw_exception_if_cyclic_dependencies_found() {
                config.bind(TestComponent.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());

                HashSet<Class<?>> classes = Sets.newHashSet(exception.getComponents());

                assertEquals(2, classes.size());
                assertTrue(classes.contains(TestComponent.class));
                assertTrue(classes.contains(Dependency.class));
            }

            // a -> b -> c -> a
            @Test
            void should_throw_exception_if_transitive_cyclic_dependencies_found() {
                config.bind(TestComponent.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);

                CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());

                List<Class<?>> components = Arrays.asList(exception.getComponents());

                assertEquals(3, components.size());
                assertTrue(components.contains(TestComponent.class));
                assertTrue(components.contains(Dependency.class));
                assertTrue(components.contains(AnotherDependency.class));
            }
        }
    }

    @Nested
    public class DependenciesSelection {

    }

    @Nested
    public class LifecycleManagement {

    }
}

interface TestComponent {
}

interface Dependency {
}

interface AnotherDependency {

}

class ComponentWithDefaultConstructor implements TestComponent {
    public ComponentWithDefaultConstructor() {
    }
}

class ComponentWithInjectConstructor implements TestComponent {
    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class ComponentWithMultiInjectConstructors implements TestComponent {
    @Inject
    public ComponentWithMultiInjectConstructors(String name, Integer value) {
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
    }
}

class ComponentWithNorDefaultInjectConstructor implements TestComponent {
    public ComponentWithNorDefaultInjectConstructor(String name) {
    }
}

class DependencyWithInjectConstructor implements Dependency {
    private String dependency;

    @Inject
    public DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}

class DependencyDependedOnComponent implements Dependency {
    private TestComponent component;

    @Inject
    public DependencyDependedOnComponent(TestComponent component) {
        this.component = component;
    }
}

class AnotherDependencyDependedOnComponent implements AnotherDependency {
    private TestComponent component;

    @Inject
    public AnotherDependencyDependedOnComponent(TestComponent component) {
        this.component = component;
    }
}

class DependencyDependedOnAnotherDependency implements Dependency {
    private AnotherDependency anotherDependency;

    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }
}