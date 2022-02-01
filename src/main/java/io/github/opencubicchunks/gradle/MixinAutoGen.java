package io.github.opencubicchunks.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.annotation.Nonnull;

public class MixinAutoGen implements Plugin<Project> {

    @Override public void apply(@Nonnull Project target) {
        MixinGenExtension extension = new MixinGenExtension();
        target.getExtensions().add("mixinGen", extension);
        target.getTasks().create("generateMixinConfigs").doLast(task -> {
            JavaPluginExtension javaExt = Utils.getJavaPluginExtension(target);
            try {
                extension.generateFiles(javaExt);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
