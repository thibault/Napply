package fr.miximum.napply;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class fr.miximum.napply.NapplyTest \
 * fr.miximum.napply.tests/android.test.InstrumentationTestRunner
 */
public class NapplyTest extends ActivityInstrumentationTestCase2<Napply> {

    public NapplyTest() {
        super("fr.miximum.napply", Napply.class);
    }

}
