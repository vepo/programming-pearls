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

        protected final File file;
        private final int lineEndSize;
        protected final long totalRecords;
        private final byte[] readBuffer;

        protected FileSorter(File file) {
            this.file = file;
            this.readBuffer = new byte[7];
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

        protected int readValue(RandomAccessFile reader, long pos) throws IOException {
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

        protected void writeValue(RandomAccessFile reader, long pos, int value) throws IOException {
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

        abstract void sort();
    }

    public static class MergeSortFileSorter extends FileSorter {

        public MergeSortFileSorter(File file) {
            super(file);
        }

        @Override
        void sort() {
            try (var reader = new RandomAccessFile(file, "rw")) {
                sort(reader, 0, totalRecords - 1);
            } catch (IOException e) {
                throw new IllegalStateException("Could not sort file!", e);
            }
        }

        private void sort(RandomAccessFile reader, long start, long end) throws IOException {
            if (start < end) {
                long middle = start + (end - start) / 2;
                sort(reader, start, middle);
                sort(reader, middle + 1, end);
                merge(reader, start, middle, end);
            }
        }

        private void merge(RandomAccessFile reader, long start, long middle, long end) throws IOException {
            int firstLength = (int) (middle - start + 1);
            int secondLength = (int) (end - middle);

            var leftFile = Files.createTempFile(null, null);
            var rightFile = Files.createTempFile(null, null);
            try (var firstFile = new RandomAccessFile(leftFile.toFile(), "rw");
                    var secondFile = new RandomAccessFile(rightFile.toFile(), "rw");) {

                for (int i = 0; i < firstLength; ++i) {
                    writeValue(firstFile, i, readValue(reader, start + i));
                }
                for (int j = 0; j < secondLength; ++j) {
                    writeValue(secondFile, j, readValue(reader, middle + 1 + j));
                }

                long firstIndex = 0;
                int firstValue = readValue(firstFile, firstIndex);
                long secondIndex = 0;
                int secondValue = readValue(secondFile, secondIndex);
                long mergeIndex = start;
                while (firstIndex < firstLength && secondIndex < secondLength) {
                    if (firstValue <= secondValue) {
                        writeValue(reader, mergeIndex++, firstValue);
                        firstValue = readValue(firstFile, ++firstIndex);
                    } else {
                        writeValue(reader, mergeIndex++, secondValue);
                        secondValue = readValue(secondFile, ++secondIndex);
                    }
                }

                while (firstIndex < firstLength) {
                    writeValue(reader, mergeIndex++, firstValue);
                    firstValue = readValue(firstFile, ++firstIndex);
                }

                while (secondIndex < secondLength) {
                    writeValue(reader, mergeIndex++, secondValue);
                    secondValue = readValue(secondFile, ++secondIndex);
                }
            } finally {
                Files.delete(leftFile);
                Files.delete(rightFile);
            }
        }
    }

    public static class MergeSortInPlaceSorter extends FileSorter {

        public MergeSortInPlaceSorter(File file) {
            super(file);
        }

        @Override
        void sort() {
            try (var reader = new RandomAccessFile(file, "rw")) {
                sort(reader, 0, totalRecords - 1);
            } catch (IOException e) {
                throw new IllegalStateException("Could not sort file!", e);
            }
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
            if (readValue(reader, middle) <= startSecondValue) {
                return;
            }

            int startFirstValue = readValue(reader, start);

            while (start <= middle && startSecond <= end) {
                if (startFirstValue <= startSecondValue) {
                    start++;
                    startFirstValue = readValue(reader, start);
                } else {
                    int value = startSecondValue;
                    long index = startSecond;

                    while (index != start) {
                        writeValue(reader, index, readValue(reader, index - 1));
                        index--;
                    }
                    writeValue(reader, start, value);

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
    public static void mergeSortInPlace10() throws IOException {
        var originFile = Paths.get("resources", "cap01", "file-001.txt");
        var outputFile = Files.createTempFile(null, null);
        Files.copy(originFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        var obviousSorter = new MergeSortInPlaceSorter(outputFile.toFile());
        obviousSorter.sort();
    }

    @Benchmark   
    @BenchmarkMode(Mode.AverageTime)
    public static void mergeSortInPlace100() throws IOException {
        var originFile = Paths.get("resources", "cap01", "file-002.txt");
        var outputFile = Files.createTempFile(null, null);
        Files.copy(originFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        var obviousSorter = new MergeSortInPlaceSorter(outputFile.toFile());
        obviousSorter.sort();
    }
    

    @Benchmark    
    @BenchmarkMode(Mode.AverageTime)
    public static void mergeSort10() throws IOException {
        var originFile = Paths.get("resources", "cap01", "file-001.txt");
        var outputFile = Files.createTempFile(null, null);
        Files.copy(originFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        var obviousSorter = new MergeSortFileSorter(outputFile.toFile());
        obviousSorter.sort();
    }

    @Benchmark    
    @BenchmarkMode(Mode.AverageTime)
    public static void mergeSort100() throws IOException {
        var originFile = Paths.get("resources", "cap01", "file-002.txt");
        var outputFile = Files.createTempFile(null, null);
        Files.copy(originFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        var obviousSorter = new MergeSortFileSorter(outputFile.toFile());
        obviousSorter.sort();
    }
}