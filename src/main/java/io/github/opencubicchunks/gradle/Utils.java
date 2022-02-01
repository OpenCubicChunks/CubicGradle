package io.github.opencubicchunks.gradle;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.JavaPluginExtension;

import javax.annotation.Nonnull;

public class Utils {

    @Nonnull public static JavaPluginExtension getJavaPluginExtension(@Nonnull Project target) {
        return (JavaPluginExtension) target.getExtensions().getByName("java");
    }
}
