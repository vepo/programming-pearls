package io.vepo.programming.pearls.cap01;

import static io.vepo.programming.pearls.cap01.CrackingTheOyster.generateFile;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.readAllLines;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Objects;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.vepo.programming.pearls.cap01.CrackingTheOyster.MergeSortInPlaceSorter;

class CrackingTheOysterTest {

    @ParameterizedTest
    @ValueSource(strings = { "\n", "\r\n" })
    void mergeSortInPlaceSorterWindows(String lineSeparator) throws IOException {
        var testFile = generateFile(createTempDirectory("test"), 0, 100, lineSeparator);
        var previousContent = readAllLines(testFile.toPath()).stream()
                .filter(line -> Objects.nonNull(line) && !line.isEmpty())
                .map(String::trim)
                .mapToInt(Integer::valueOf)
                .boxed()
                .sorted()
                .toList();
        var sorter = new MergeSortInPlaceSorter(testFile);
        sorter.sort();
        Integer previousValue = null;
        for (var line : readAllLines(testFile.toPath())) {
            if (nonNull(line) && !line.isEmpty()) {
                int currentValue = Integer.valueOf(line);
                if (nonNull(previousValue)) {
                    assertThat(currentValue).isGreaterThan(previousValue);
                }
                previousValue = currentValue;
            }
        }
        var currentContent = readAllLines(testFile.toPath()).stream()
                .filter(line -> Objects.nonNull(line) && !line.isEmpty())
                .map(String::trim)
                .mapToInt(Integer::valueOf)
                .boxed()
                .sorted()
                .toList();
        assertThat(currentContent).isEqualTo(previousContent);
    }

}
