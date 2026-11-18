/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */
package org.roaringbitmap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/** Stable unsigned-prefix radix sort regression including already sorted input. */
public class PartialRadixSortedFastPathTest {

  @Test
  public void emptySingletonAndSortedPrefixesAreUnchanged() {
    int[][] cases = {
      {},
      {Integer.MIN_VALUE},
      {0x000000FF, 0x00000000, 0x7FFFFFFF, 0x80000001, 0xFFFF0001},
      {0x00000001, 0x00000002, 0x00000000},
      {0xFFFF0000, 0xFFFF0002, 0xFFFF0001}
    };
    for (int[] input : cases) {
      int[] original = input.clone();
      Util.partialRadixSort(input);
      assertArrayEquals(original, input);
    }
  }

  @Test
  public void unsortedPrefixesMatchStableReference() {
    Random random = new Random(0x524F4152494E47L);
    for (int trial = 0; trial < 3000; trial++) {
      int[] input = new int[random.nextInt(150)];
      for (int i = 0; i < input.length; i++) {
        input[i] = random.nextInt();
      }
      Integer[] stable = Arrays.stream(input).boxed().toArray(Integer[]::new);
      Arrays.sort(stable, Comparator.comparingInt(value -> value >>> 16));
      int[] expected = Arrays.stream(stable).mapToInt(Integer::intValue).toArray();
      Util.partialRadixSort(input);
      assertArrayEquals(expected, input);
    }
  }

  @Test
  public void nullStillThrows() {
    assertThrows(NullPointerException.class, () -> Util.partialRadixSort(null));
  }
}
