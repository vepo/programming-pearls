package io.vepo.programming.pearls.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.codec.digest.DigestUtils;

public class Md5Checksum {
    private final File file;
    private AtomicReference<String> checksum;

    public Md5Checksum(File file) {
        this.file = file;
        checksum = new AtomicReference<>();
    }

    public String get() {
        return checksum.updateAndGet(value -> loadChecksum(value));
    }

    private String loadChecksum(String value) {
        if (Objects.isNull(value)) {
            try (var is = Files.newInputStream(file.toPath())) {
                value = DigestUtils.md5Hex(is);
            } catch (IOException ioe) {
                throw new IllegalStateException("Could not load MD5 Checksum!", ioe);
            }
        }
        return value;
    }
}
