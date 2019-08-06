package io.github.opencubicchunks.gradle;

public class McGitVersionExtension {

    boolean configured;
    private Boolean snapshot;
    private String versionSuffix = "";
    private String forceVersionString;

    public boolean isSnapshot() {
        return snapshot;
    }

    public void setSnapshot(boolean snapshot) {
        this.configured = true;
        this.snapshot = snapshot;
    }

    public String getVersionSuffix() {
        return versionSuffix;
    }

    public void setVersionSuffix(String versionSuffix) {
        this.configured = true;
        this.versionSuffix = versionSuffix;
    }

    public String getForceVersionString() {
        return forceVersionString;
    }

    public void setForceVersionString(String forceVersionString) {
        this.configured = true;
        this.forceVersionString = forceVersionString;
    }
}
