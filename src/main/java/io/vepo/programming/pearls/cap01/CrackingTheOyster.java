package io.vepo.programming.pearls.cap01;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;

public class CrackingTheOyster {
    public static abstract class FileSorter {
        abstract void sort();
    }

    public static class MergeSortInPlaceSorter extends FileSorter {

        private final File file;
        private final int lineEndSize;
        private final long totalRecords;
        private final byte[] readBuffer = new byte[7];

        public MergeSortInPlaceSorter(File file) {
            this.file = file;
            try (var reader = new RandomAccessFile(file, "rw")) {
                var lineEnds = new byte[2];
                reader.seek(7);
                reader.read(lineEnds, 0, 2);
                if (lineEnds[0] == '\n' && lineEnds[1] >= '0' && lineEnds[1] <= '9') {
                    lineEndSize = 1;
                } else if (lineEnds[0] == '\r' && lineEnds[1] == '\n') {
                    lineEndSize = 2;
                } else {
                    throw new IllegalStateException("Not a valid line end!");
                }
                totalRecords = reader.length() / (7 + lineEndSize);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("File not found!", e);
            } catch (IOException e) {
                throw new IllegalStateException("Cannor read file!", e);
            }
        }

        @Override
        void sort() {
            try (var reader = new RandomAccessFile(file, "rw")) {
                sort(reader, 0, totalRecords - 1);
            } catch (IOException e) {
                throw new IllegalStateException("Could not sort file!", e);
            }
        }

        private int readValue(RandomAccessFile reader, long pos) throws IOException {
            reader.seek(pos * (7 + lineEndSize));
            reader.read(readBuffer, 0, 7);
            return readBuffer[6] - '0' +
                    ((readBuffer[5] - '0') * 10) +
                    ((readBuffer[4] - '0') * 100) +
                    ((readBuffer[3] - '0') * 1000) +
                    ((readBuffer[2] - '0') * 10000) +
                    ((readBuffer[1] - '0') * 100000) +
                    ((readBuffer[0] - '0') * 1000000);
        }

        private void writeValue(RandomAccessFile reader, long pos, int value) throws IOException {
            reader.seek(pos * (7 + lineEndSize));
            readBuffer[0] = (byte) ('0' + (value / 1000000));
            readBuffer[1] = (byte) ('0' + ((value % 1000000) / 100000));
            readBuffer[2] = (byte) ('0' + ((value % 100000) / 10000));
            readBuffer[3] = (byte) ('0' + ((value % 10000) / 1000));
            readBuffer[4] = (byte) ('0' + ((value % 1000) / 100));
            readBuffer[5] = (byte) ('0' + ((value % 100) / 10));
            readBuffer[6] = (byte) ('0' + (value % 10));
            reader.write(readBuffer);
        }

        private void sort(RandomAccessFile reader, long start, long end) throws IOException {
            if (start < end) {
                // Find the middle point
                long middle = start + (end - start) / 2;
                sort(reader, start, middle);
                sort(reader, middle + 1, end);
                merge(reader, start, middle, end);
            }
        }

        private void merge(RandomAccessFile reader, long start, long middle, long end) throws IOException {
            long startSecond = middle + 1;
            int startSecondValue = readValue(reader, startSecond);
            // If the direct merge is already sorted
            if (readValue(reader, middle) <= startSecondValue) {
                return;
            }

            int startFirstValue = readValue(reader, start);

            // Two pointers to maintain start
            // of both arrays to merge
            while (start <= middle && startSecond <= end) {

                // If element 1 is in right place
                if (startFirstValue <= startSecondValue) {
                    start++;
                    startFirstValue = readValue(reader, start);
                } else {
                    int value = startSecondValue;
                    long index = startSecond;

                    // Shift all the elements between element 1
                    // element 2, right by 1.
                    while (index != start) {
                        writeValue(reader, index, readValue(reader, index - 1));
                        index--;
                    }
                    writeValue(reader, start, value);

                    // Update all the pointers
                    start++;
                    middle++;
                    startSecond++;
                    startFirstValue = readValue(reader, start);
                    startSecondValue = readValue(reader, startSecond);
                }
            }
        }

    }

    public static void main(String[] args) throws IOException {
        generateFile(Paths.get("resources", "cap01"), 1, 10, "\r");
        generateFile(Paths.get("resources", "cap01"), 2, 100, "\r");
        generateFile(Paths.get("resources", "cap01"), 3, 10_00, "\r");
        generateFile(Paths.get("resources", "cap01"), 4, 10_000, "\r");
        // var originFile = Paths.get("resources", "cap01", "file-000.txt");
        // var outputFile = Files.createTempFile(null, null);
        // Files.copy(originFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        // var obviousSorter = new ObviousSolutionFileSorter(outputFile.toFile());
        // obviousSorter.sort();
        // System.out.println("========= BEFORE =========");
        // Files.readAllLines(originFile).forEach(System.out::println);
        // System.out.println("========= AFTER ========");
        // Files.readAllLines(outputFile).forEach(System.out::println);
    }

    public static File generateFile(Path directory, int index, int length, String lineSeparator)
            throws FileNotFoundException, UnsupportedEncodingException {
        var maxValue = 9_999_999;
        var outputFile = directory.resolve(String.format("file-%03d.txt", index)).toFile();
        if (outputFile.exists()) {
            outputFile.delete();
        }

        var usedNumbers = new boolean[maxValue + 1];
        Arrays.fill(usedNumbers, false);
        var numberGenerator = new SecureRandom();
        try (var writer = new PrintWriter(outputFile, "UTF-8")) {
            IntStream.range(0, length)
                    .sequential()
                    .forEach(i -> {
                        var number = numberGenerator.nextInt(maxValue + 1);
                        while (usedNumbers[number]) {
                            number = numberGenerator.nextInt(maxValue + 1);
                        }
                        usedNumbers[number] = true;
                        writer.print(String.format("%07d", number));
                        writer.print(lineSeparator);
                    });
        }
        return outputFile;
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void mergeSortInPlace10() throws IOException {
        var originFile = Paths.get("resources", "cap01", "file-001.txt");
        var outputFile = Files.createTempFile(null, null);
        Files.copy(originFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        var obviousSorter = new MergeSortInPlaceSorter(outputFile.toFile());
        obviousSorter.sort();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void mergeSortInPlace100() throws IOException {
        var originFile = Paths.get("resources", "cap01", "file-002.txt");
        var outputFile = Files.createTempFile(null, null);
        Files.copy(originFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        var obviousSorter = new MergeSortInPlaceSorter(outputFile.toFile());
        obviousSorter.sort();
    }
}