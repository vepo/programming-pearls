package io.vepo.programming.pearls;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import io.vepo.programming.pearls.cap01.CrackingTheOyster;

public class BenchmarkRunner {
    public static void main(String[] args) throws Exception {
        URLClassLoader classLoader = (URLClassLoader) BenchmarkRunner.class.getClassLoader();
        StringBuilder classpath = new StringBuilder();
        for (URL url : classLoader.getURLs()) {
            classpath.append(url.getPath()).append(File.pathSeparator);
        }
        System.setProperty("java.class.path", classpath.toString());
        Options opt = new OptionsBuilder()
                                          .include(CrackingTheOyster.class.getSimpleName())
                                          .build();

        new Runner(opt).run();
    }
}
