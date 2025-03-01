package io.vepo.programming.pearls;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import io.vepo.programming.pearls.cap01.CrackingTheOyster;

public class BenchmarkRunner {
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(CrackingTheOyster.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
