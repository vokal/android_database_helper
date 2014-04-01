package com.vokal.db.test;

import android.test.suitebuilder.TestSuiteBuilder;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AndroidTests extends TestSuite {
    public static Test suite(){
        return new TestSuiteBuilder(AndroidTests.class)
                .includeAllPackagesUnderHere()
                .build();
    }
}
