package com.anurag.itds.dataset;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Imports bundled datasets on startup when {@code safewalk.datasets.import-on-startup=true}. */
@Component
@ConditionalOnProperty(name = "safewalk.datasets.import-on-startup", havingValue = "true")
public class DatasetImportRunner implements ApplicationRunner {

    private final DatasetImporter importer;

    public DatasetImportRunner(DatasetImporter importer) {
        this.importer = importer;
    }

    @Override
    public void run(ApplicationArguments args) {
        importer.importAll();
    }
}
