/*
* Copyright (c) IBM Corporation 2017. All Rights Reserved.
* Project name: java-async-util
* This project is licensed under the Apache License 2.0, see LICENSE.
*/

package com.ibm.asyncutil.locks;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.asyncutil.util.Combinators;
import com.ibm.asyncutil.util.TestUtil;

public abstract class AbstractAsyncNamedReadWriteLockTest extends AbstractAsyncReadWriteLockTest {

  protected abstract <T> AsyncNamedReadWriteLock<T> getNamedReadWriteLock();

  protected abstract boolean isEmpty(AsyncNamedReadWriteLock<?> narwls);

  @Override
  protected final AsyncReadWriteLock getReadWriteLock() {
    return new AsyncNamedRWLockAsRWLock(getNamedReadWriteLock());
  }

  @Test
  public final void testExclusivity() throws Exception {
    final AsyncNamedReadWriteLock<String> narwls = getNamedReadWriteLock();
    final String s1 = "abc";
    final String s2 = "123";

    Assert.assertTrue(isEmpty(narwls));
    final CompletableFuture<AsyncReadWriteLock.ReadLockToken> read1_1 =
        narwls.acquireReadLock(s1).toCompletableFuture();
    Assert.assertFalse(isEmpty(narwls));
    Assert.assertTrue(read1_1.isDone());

    final CompletableFuture<AsyncReadWriteLock.ReadLockToken> read1_2 =
        narwls.acquireReadLock(s1).toCompletableFuture();
    Assert.assertTrue(read1_2.isDone());

    final CompletableFuture<AsyncReadWriteLock.WriteLockToken> write1_1 =
        narwls.acquireWriteLock(s1).toCompletableFuture();
    Assert.assertFalse(write1_1.isDone());

    TestUtil.join(read1_2).releaseLock();
    Assert.assertFalse(write1_1.isDone());

    // s2 can read or write while s1 is occupied
    Assert.assertTrue(narwls.acquireReadLock(s2)
        .thenAccept(readLock -> readLock.releaseLock()).toCompletableFuture().isDone());
    Assert.assertTrue(narwls.acquireWriteLock(s2)
        .thenAccept(writeLock -> writeLock.releaseLock()).toCompletableFuture().isDone());
    Assert.assertTrue(narwls.acquireReadLock(s2)
        .thenAccept(readLock -> readLock.releaseLock()).toCompletableFuture().isDone());
    Assert.assertFalse(isEmpty(narwls));

    TestUtil.join(read1_1).releaseLock();
    Assert.assertTrue(write1_1.isDone());

    Assert.assertFalse(narwls.acquireReadLock(s1)
        .thenAccept(readLock -> readLock.releaseLock()).toCompletableFuture().isDone());

    TestUtil.join(write1_1).releaseLock();
    Assert.assertTrue(isEmpty(narwls));
  }

  private static class MutableKey {
    int value = 0;

    @Override
    public int hashCode() {
      return this.value;
    }
  }

  @Test(expected = ConcurrentModificationException.class)
  public final void testReadNameModificationException() {
    final AsyncNamedReadWriteLock<MutableKey> lock = getNamedReadWriteLock();
    final MutableKey key = new MutableKey();
    final AsyncReadWriteLock.ReadLockToken readToken = TestUtil.join(lock.acquireReadLock(key));
    key.value = 1;
    readToken.releaseLock();
  }

  @Test(expected = ConcurrentModificationException.class)
  public final void testWriteNameModificationException() {
    final AsyncNamedReadWriteLock<MutableKey> lock = getNamedReadWriteLock();
    final MutableKey key = new MutableKey();
    final AsyncReadWriteLock.WriteLockToken writeToken = TestUtil.join(lock.acquireWriteLock(key));
    key.value = 1;
    writeToken.releaseLock();
  }

  public static abstract class AbstractAsyncNamedReadWriteLockFairnessTest
      extends AbstractAsyncNamedReadWriteLockTest {
    @Test
    public void testFairness() throws Exception {
      final AsyncNamedReadWriteLock<String> narwls = getNamedReadWriteLock();
      final String s = "abc";

      final CompletableFuture<AsyncReadWriteLock.WriteLockToken> write1 =
          narwls.acquireWriteLock(s).toCompletableFuture();
      Assert.assertTrue(write1.isDone());

      // under write lock
      final CompletableFuture<AsyncReadWriteLock.ReadLockToken> read1 =
          narwls.acquireReadLock(s).toCompletableFuture();
      final CompletableFuture<AsyncReadWriteLock.ReadLockToken> read2 =
          narwls.acquireReadLock(s).toCompletableFuture();
      final CompletableFuture<AsyncReadWriteLock.ReadLockToken> read3 =
          narwls.acquireReadLock(s).toCompletableFuture();
      Assert.assertFalse(read1.isDone());
      Assert.assertFalse(read2.isDone());
      Assert.assertFalse(read3.isDone());

      final CompletableFuture<AsyncReadWriteLock.WriteLockToken> write2 =
          narwls.acquireWriteLock(s).toCompletableFuture();
      Assert.assertFalse(write2.isDone());

      final CompletableFuture<AsyncReadWriteLock.ReadLockToken> read4 =
          narwls.acquireReadLock(s).toCompletableFuture();
      final CompletableFuture<AsyncReadWriteLock.ReadLockToken> read5 =
          narwls.acquireReadLock(s).toCompletableFuture();
      final CompletableFuture<AsyncReadWriteLock.ReadLockToken> read6 =
          narwls.acquireReadLock(s).toCompletableFuture();
      Assert.assertFalse(read4.isDone());
      Assert.assertFalse(read5.isDone());
      Assert.assertFalse(read6.isDone());

      final CompletableFuture<AsyncReadWriteLock.WriteLockToken> write3 =
          narwls.acquireWriteLock(s).toCompletableFuture();
      Assert.assertFalse(write3.isDone());

      TestUtil.join(write1).releaseLock();
      Assert.assertTrue(read1.isDone());
      Assert.assertTrue(read2.isDone());
      Assert.assertTrue(read3.isDone());
      Assert.assertFalse(write2.isDone());
      Assert.assertFalse(read4.isDone());
      Assert.assertFalse(read5.isDone());
      Assert.assertFalse(read6.isDone());
      Assert.assertFalse(write3.isDone());

      TestUtil.join(read1).releaseLock();
      TestUtil.join(read2).releaseLock();
      Assert.assertTrue(read3.isDone());
      Assert.assertFalse(write2.isDone());
      Assert.assertFalse(read4.isDone());
      Assert.assertFalse(read5.isDone());
      Assert.assertFalse(read6.isDone());
      Assert.assertFalse(write3.isDone());

      // now under read lock (read3 still active)
      final CompletableFuture<AsyncReadWriteLock.ReadLockToken> read7 =
          narwls.acquireReadLock(s).toCompletableFuture();
      final CompletableFuture<AsyncReadWriteLock.WriteLockToken> write4 =
          narwls.acquireWriteLock(s).toCompletableFuture();

      Assert.assertTrue(read3.isDone());
      Assert.assertFalse(write2.isDone());
      Assert.assertFalse(read4.isDone());
      Assert.assertFalse(read5.isDone());
      Assert.assertFalse(read6.isDone());
      Assert.assertFalse(write3.isDone());
      Assert.assertFalse(read7.isDone());
      Assert.assertFalse(write4.isDone());

      TestUtil.join(read3).releaseLock();
      Assert.assertTrue(write2.isDone());
      Assert.assertFalse(read4.isDone());
      Assert.assertFalse(read5.isDone());
      Assert.assertFalse(read6.isDone());
      Assert.assertFalse(write3.isDone());
      Assert.assertFalse(read7.isDone());
      Assert.assertFalse(write4.isDone());

      TestUtil.join(write2).releaseLock();
      Assert.assertTrue(read4.isDone());
      Assert.assertTrue(read5.isDone());
      Assert.assertTrue(read6.isDone());
      Assert.assertFalse(write3.isDone());
      Assert.assertFalse(read7.isDone());
      Assert.assertFalse(write4.isDone());

      TestUtil.join(Combinators.collect(Arrays.asList(read4, read5, read6)))
          .forEach(readLock -> readLock.releaseLock());
      Assert.assertTrue(write3.isDone());
      Assert.assertFalse(read7.isDone());
      Assert.assertFalse(write4.isDone());

      TestUtil.join(write3).releaseLock();
      Assert.assertTrue(read7.isDone());
      Assert.assertFalse(write4.isDone());

      TestUtil.join(read7).releaseLock();
      Assert.assertTrue(write4.isDone());

      TestUtil.join(write4).releaseLock();
      Assert.assertTrue(isEmpty(narwls));
    }
  }
}


class AsyncNamedRWLockAsRWLock implements AsyncReadWriteLock {
  private static final Object KEY = new Object();
  private final AsyncNamedReadWriteLock<Object> lock;

  public AsyncNamedRWLockAsRWLock(final AsyncNamedReadWriteLock<Object> lock) {
    this.lock = lock;
  }

  @Override
  public CompletionStage<ReadLockToken> acquireReadLock() {
    return this.lock.acquireReadLock(KEY);
  }

  @Override
  public CompletionStage<WriteLockToken> acquireWriteLock() {
    return this.lock.acquireWriteLock(KEY);
  }

  @Override
  public Optional<ReadLockToken> tryReadLock() {
    return this.lock.tryReadLock(KEY);
  }

  @Override
  public Optional<WriteLockToken> tryWriteLock() {
    return this.lock.tryWriteLock(KEY);
  }

}
