package play.deps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

import org.apache.commons.io.FileUtils;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.plugins.repository.Resource;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import play.Play;
import play.PlayBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.assertj.core.api.Assumptions.assumeThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class YamlParserTest {

    private static final List<String> DEPENDENCIES = List.of("crud", "deadbolt-1.5.4", "pdf-1.5");
    private static final List<String> REVERSED_DEPENDENCIES;
    static {
        var reversed = new ArrayList<>(DEPENDENCIES);
        Collections.reverse(reversed);

        REVERSED_DEPENDENCIES = List.copyOf(reversed);
    }

    @BeforeAll static void setUp() {
        // Play
        new PlayBuilder().build();
        System.setProperty("play.version", Play.version);
        
        // We will create a "tmp/modules" directory to simulate the play dependencies
        File modules = new File(Play.applicationPath, "modules");
        if (!modules.exists()) {
            assumeThat(modules.mkdirs())
                .as("create '%s' folder", modules.getName())
                .isTrue();
        }

        for (String dependency : DEPENDENCIES) {
            var module = new File(modules, dependency);
            if (!module.exists()) {
                assumeThatThrownBy(() -> assumeThat(module.createNewFile()).isTrue())
                    .doesNotThrowAnyException();
            }
        }
    }

    @AfterAll static void cleanUp() throws Exception {
        File moduleDir = new File(Play.applicationPath, "modules");
        FileUtils.deleteDirectory(moduleDir);
    }

    @Test void fileNotFoundTest() {
        assertThatThrownBy(() -> YamlParser.getOrderedModuleList(new File(Play.applicationPath, "fakeFile.yml")))
            .isInstanceOf(FileNotFoundException.class)
            .hasMessageStartingWith("There was a problem to find the file");
    }

    @Test void retrieveModulesTest() throws Exception {
        Set<String> modules = YamlParser.getOrderedModuleList(new File(getClass().getResource("/play/deps/dependencies_test1.yml").toURI()));
        assertThat(modules).containsExactlyElementsOf(DEPENDENCIES);
    }
    
    @Test void retrieveModulesTest2() throws Exception {
        Set<String> modules = YamlParser.getOrderedModuleList(new File(getClass().getResource("/play/deps/dependencies_test2.yml").toURI()));
        assertThat(modules).containsExactlyElementsOf(REVERSED_DEPENDENCIES);
    }

    @Test void transitiveDependenciesFalseTest() throws Exception {
        Resource resource = mock(Resource.class);
        try (InputStream inputStream = getClass().getResource("dependencies_test_transitiveDependencies.yml").openStream()) {
            when(resource.openStream()).thenReturn(inputStream);

            YamlParser yamlParser = new YamlParser();
            ModuleDescriptor moduleDescriptor = yamlParser.parseDescriptor(
                null /*unused*/,
                null /*unused*/,
                resource,
                false /*unused*/
            );
            assertThat(moduleDescriptor)
                .isNotNull()
                .extracting(ModuleDescriptor::getDependencies, InstanceOfAssertFactories.array(DependencyDescriptor[].class))
                .hasSize(2)
                .allMatch(dependency -> !dependency.isTransitive());
        }
    }

}
