/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.roaringbitmap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

/** Differential skip-offset oracle: short skips, zero progress, EOF and byte fallback. */
public class SkipFullyPartialDataInputTest {
  private static final class PartialDataInput {
    final DataInput input;
    int skipCalls;
    int readCalls;

    PartialDataInput(byte[] values, int maxSkip) {
      DataInputStream base = new DataInputStream(new ByteArrayInputStream(values));
      input = (DataInput) Proxy.newProxyInstance(
          PartialDataInput.class.getClassLoader(), new Class<?>[] {DataInput.class},
          (proxy, method, args) -> {
            if (method.getName().equals("skipBytes")) {
              skipCalls++;
              return base.skipBytes(Math.min((Integer) args[0], maxSkip));
            }
            if (method.getName().equals("readByte")) {
              readCalls++;
              return base.readByte();
            }
            throw new AssertionError("unexpected method " + method.getName());
          });
    }
  }

  @Test
  public void bytePositionMatchesEveryPartialSkipWindow() throws IOException {
    for (int length = 0; length < 80; length++) {
      byte[] values = new byte[length];
      for (int i = 0; i < length; i++) {
        values[i] = (byte) (i ^ 0x5a);
      }
      for (int requested = 0; requested <= length; requested++) {
        for (int cap : new int[] {0, 1, 3, 7, 31}) {
          PartialDataInput cursor = new PartialDataInput(values, cap);
          Util.skipFully(cursor.input, requested);
          if (requested < length) {
            assertEquals(values[requested], cursor.input.readByte());
          } else {
            assertThrows(EOFException.class, cursor.input::readByte);
          }
        }
      }
    }
  }

  @Test
  public void repeatedPartialSkipsAvoidPerByteFallback() throws IOException {
    byte[] bytes = new byte[8192];
    PartialDataInput cursor = new PartialDataInput(bytes, 31);
    Util.skipFully(cursor.input, bytes.length);
    assertEquals(0, cursor.readCalls);
    assertTrue(cursor.skipCalls > 1);
    assertThrows(EOFException.class, cursor.input::readByte);
  }

  @Test
  public void zeroProgressUsesOneByteReadsAndChecksEof() throws IOException {
    byte[] bytes = new byte[45];
    PartialDataInput cursor = new PartialDataInput(bytes, 0);
    Util.skipFully(cursor.input, 31);
    assertEquals(31, cursor.readCalls);
    assertEquals(0, cursor.input.readByte());
    PartialDataInput exhausted = new PartialDataInput(bytes, 0);
    assertThrows(EOFException.class, () -> Util.skipFully(exhausted.input, 46));
    assertEquals(bytes.length + 1, exhausted.readCalls);
  }
}
