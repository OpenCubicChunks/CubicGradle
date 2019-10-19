package io.github.opencubicchunks.gradle;

import net.minecraftforge.gradle.user.patcherUser.forge.ForgeExtension;
import org.ajoberstar.grgit.Grgit;
import org.ajoberstar.grgit.operation.DescribeOp;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class McGitVersion implements Plugin<Project> {

    @Override public void apply(Project target) {
        McGitVersionExtension extension = new McGitVersionExtension();
        target.getExtensions().add("mcGitVersion", extension);
        extension.configured = true;
        target.setVersion(lazyString(() -> {
            String version = getVersion(extension, target, false);
            target.getLogger().lifecycle("Auto-detected version {}", version);
            return version;
        }));
        target.getExtensions().getExtraProperties().set("mavenProjectVersion", lazyString(() -> {
            String version = getVersion(extension, target, true);
            target.getLogger().lifecycle("Auto-detected version for maven {}", version);
            return version;
        }));
    }

    private String getVersion(McGitVersionExtension extension, Project target, boolean maven) {
        if (extension.getForceVersionString() != null) {
            return extension.getForceVersionString();
        }
        if (!extension.configured) {
            throw new IllegalStateException("Accessing project version before mcGitVersion is configured! Configure mcGitVersion first!");
        }
        try {
            Grgit git = openRepository(target.getProjectDir().toPath());
            String describe = new DescribeOp(git.getRepository()).call();
            String branch = getGitBranch(git);
            String snapshotSuffix = extension.isSnapshot() ? "-SNAPSHOT" : "";
            return getModVersion(target, extension, describe, branch, maven) + snapshotSuffix;
        } catch (RuntimeException | RepositoryNotFoundException ex) {
            target.getLogger().error("Unknown error when accessing git repository! Are you sure the git repository exists?", ex);
            return String.format("%s-%s.%s.%s%s%s", getMcVersion(target), "9999", "9999", "9999", "", "NOVERSION");
        }
    }

    private Grgit openRepository(Path path) throws RepositoryNotFoundException {
        while (path.getParent() != null) {
            try {
                // this allows javac to accept catching that exception (thrown by GrGit.open, but not declared with "throws")
                //noinspection ConstantConditions
                if (false) throw new RepositoryNotFoundException((File) null);
                Path pathFinal = path;
                return Grgit.open(openOp -> openOp.setDir(pathFinal));
            } catch (RepositoryNotFoundException ignored) {
                path = path.getParent();
            }
        }
        throw new RepositoryNotFoundException(path.toFile());
    }

    private String getMcVersion(Project target) {
        ForgeExtension minecraft = target.getExtensions().getByType(ForgeExtension.class);
        return minecraft.getVersion().split("-")[0];
    }

    private String getGitBranch(Grgit git) {
        String branch = git.getBranch().current().getName();
        if (branch.equals("HEAD")) {
            branch = firstNonEmpty(
                    () -> new RuntimeException("Found HEAD branch! This is most likely caused by detached head state! Will assume unknown version!"),
                    System.getenv("TRAVIS_BRANCH"),
                    System.getenv("GIT_BRANCH"),
                    System.getenv("BRANCH_NAME"),
                    System.getenv("GITHUB_HEAD_REF")
            );
        }

        if (branch.startsWith("origin/")) {
            branch = branch.substring("origin/".length());
        }
        return branch;
    }


    private String getModVersion(Project target, McGitVersionExtension extension, String describe, String branch, boolean mvn) {
        String mcVersion = getMcVersion(target);
        if (branch.startsWith("MC_")) {
            String branchMcVersion = branch.substring("MC_".length());
            if (!mcVersion.startsWith(branchMcVersion)) {
                target.getLogger().warn("Branch version different than project MC version! MC version: " +
                        mcVersion + ", branch: " + branch + ", branch version: " + branchMcVersion);
            }
        }

        //branches "master" and "MC_something" are not appended to version sreing, everything else is
        //only builds from "master" and "MC_version" branches will actually use the correct versioning
        //but it allows to distinguish between builds from different branches even if version number is the same
        String branchSuffix = (branch.equals("master") || branch.startsWith("MC_")) ? "" : ("-" + branch.replaceAll("[^a-zA-Z0-9.-]", "_"));

        Pattern baseVersionRegex = Pattern.compile("v[0-9]+\\.[0-9]+");
        String versionSuffix = extension.getVersionSuffix();
        String unknownVersion = String.format("%s-UNKNOWN_VERSION%s%s", mcVersion, versionSuffix, branchSuffix);
        if (!describe.contains("-")) {
            //is it the "vX.Y" format?
            if (baseVersionRegex.matcher(describe).matches()) {
                return mvn ? String.format("%s-%s", mcVersion, describe) :
                        String.format("%s-%s.0.0%s%s", mcVersion, describe, versionSuffix, branchSuffix);
            }
            target.getLogger().error("Git describe information: " + describe + " in unknown/incorrect format");
            return unknownVersion;
        }
        //Describe format: vX.Y-build-hash
        String[] parts = describe.split("-");
        if (!baseVersionRegex.matcher(parts[0]).matches()) {
            target.getLogger().error("Git describe information: " + describe + " in unknown/incorrect format");
            return unknownVersion;
        }
        if (!parts[1].matches("[0-9]+")) {
            target.getLogger().error("Git describe information: " + describe + " in unknown/incorrect format");
            return unknownVersion;
        }
        String modAndApiVersion = parts[0].substring(1);

        int minor = Integer.parseInt(parts[1]);
        int patch = 0;

        return (mvn) ? String.format("%s-%s%s", mcVersion, modAndApiVersion, versionSuffix)
                : String.format("%s-%s.%d.%d%s%s", mcVersion, modAndApiVersion, minor, patch, versionSuffix, branchSuffix);
    }

    private static String firstNonEmpty(Supplier<RuntimeException> exception, String... items) {
        for (String i : items) {
            if (i != null && !i.isEmpty()) {
                return i;
            }
        }
        throw exception.get();
    }

    private Object lazyString(Supplier<String> getString) {
        return new Object() {
            private String cached;

            @Override public String toString() {
                if (cached == null) {
                    cached = getString.get();
                }
                return cached;
            }
        };
    }
}
