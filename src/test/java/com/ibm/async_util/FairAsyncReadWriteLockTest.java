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
// Date: May 25, 2016
// ---------------------

package com.ibm.async_util;

import com.ibm.async_util.AbstractAsyncReadWriteLockTest.AbstractAsyncReadWriteLockFairnessTest;

public class FairAsyncReadWriteLockTest
    extends AbstractAsyncReadWriteLockFairnessTest {
  @Override
  protected AsyncReadWriteLock getReadWriteLock() {
    return new FairAsyncReadWriteLock();
  }
}

