//
// (C) Copyright IBM Corp. 2005 All Rights Reserved.
//
// Contact Information:
//
// IBM Corporation
// Legal Department
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// END-OF-HEADER
//
// -----------------------
// @author: renar
//
// Date: Apr 10, 2016
// ---------------------

package com.ibm.async_util;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import com.ibm.async_util.AsyncReadWriteLock.ReadLockToken;
import com.ibm.async_util.AsyncReadWriteLock.WriteLockToken;

/**
 * A mechanism used to acquire read-write locks shared by a common name. Acquisitions for a given
 * name will share exclusivity with other acquisitions of the same name, based on
 * {@link Object#equals(Object) object equality}. Acquisitions of different names may proceed
 * unobstructed.
 * <p>
 * Implementations will specify whether their lock acquisition is fair or not; this interface does
 * not define this requirement.
 * <p>
 * Note that implementations will generally employ an underlying {@link java.util.Map}; as such, the
 * same precautions must be taken regarding mutability of keys (names). Name objects should not
 * change from the time of acquisition to the time of release, with respect to their
 * {@link Object#equals(Object) equality} and {@link Object#hashCode() hash code} semantics. The
 * release methods of the returned {@link ReadLockToken} and {@link WriteLockToken} may throw a
 * {@link java.util.ConcurrentModificationException} if such a modification is detected.
 * 
 * @param <T> the type of named objects used to identify read-write locks
 */
public interface AsyncNamedReadWriteLock<T> {

  /**
   * Acquire the read-lock associated with the given name. If the associated write-lock is not
   * currently held, the returned future will be immediately complete. Otherwise, the returned
   * future will complete when the lock is no longer exclusively acquired by a writer.
   * <p>
   * The {@link ReadLockToken} held by the returned future is used to release the read lock after it
   * has been acquired and the read-lock-protected action has completed.
   */
  public CompletionStage<ReadLockToken> acquireReadLock(T name);

  /**
   * Exclusively acquire the write-lock associated with the given name. If the associated write-lock
   * or read-lock is not currently held, the returned future will be immediately complete.
   * Otherwise, the returned future will complete when the lock is no longer held by any readers or
   * an exclusive writer.
   * <p>
   * The {@link WriteLockToken} held by the returned future is used to release the write lock after
   * it has been acquired and the write-lock-protected action has completed.
   */
  public CompletionStage<WriteLockToken> acquireWriteLock(T name);

  /**
   * Attempt to acquire the read-lock associated with the given name. If the associated write-lock
   * is not currently held, the returned Optional will hold a ReadLockToken representing this
   * acquisition. Otherwise, the returned Optional will be empty.
   * <p>
   * The {@link ReadLockToken} held by the returned optional is used to release the read lock after
   * it has been acquired and the read-lock-protected action has completed.
   */
  public Optional<ReadLockToken> tryReadLock(T name);

  /**
   * Attempt to acquire the write-lock associated with the given name. If the associated write-lock
   * or read-lock is not currently held, the returned Optional will hold a WriteLockToken
   * representing this acquisition. Otherwise, the returned Optional will be empty.
   * <p>
   * The {@link WriteLockToken} held by the returned future is used to release the write lock after
   * it has been acquired and the write-lock-protected action has completed.
   */
  public Optional<WriteLockToken> tryWriteLock(T name);
}
