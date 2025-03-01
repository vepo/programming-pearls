# Programming Pearls

## Cap 01 - Cracking the Oyster

### Problem Statement

```
Input:       A file containing at most n positive integer, each less than n, where 
             n=10^7. It is fatal error if any integer occurs twice in the input. No
             other data is associated with the integer.
Output:      A sorted list in increasing order of the input integers.
Constraints: At most (roughly) a megabyte of storage is available in the main memory;
             ample disk storage is available. The run time can be at most several
             minutes; a run time of ten secoonds need not  be decresased.
```

### Solutions

1. Obvious: Merge Sort In-Place
   ```
   CrackingTheOyster.mergeSortInPlace10   avgt   25  0.002 ±  0.001   s/op
   CrackingTheOyster.mergeSortInPlace100  avgt   25  0.025 ±  0.003   s/op
   ```
2. Obvious: Merge Sorte with auxiliar file