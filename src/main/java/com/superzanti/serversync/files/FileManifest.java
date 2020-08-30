package com.superzanti.serversync.files;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FileManifest implements Serializable {
    public List<String> directories = new ArrayList<>();
    /**
     * The files that the server wants to manage.
     */
    public List<ManifestEntry> entries = new ArrayList<>();
}
