package io.plotnik.freewriting;

import java.io.File;
import java.time.LocalDate;

public class FwDate {

    LocalDate date;
    File path;
    boolean root;

    public FwDate(LocalDate date, File path, boolean root) {
        this.date = date;
        this.path = path;
        this.root = root;
    }

    public LocalDate getDate() {
        return date;
    }

    public File getPath() {
        return path;
    }

    public boolean isRoot() {
        return root;
    }
    
}
