
package fr.miximum.napply.tests;

import fr.miximum.napply.WidgetConfigure;
import fr.miximum.picker.NumberPicker;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class WidgetConfigureTest extends ActivityInstrumentationTestCase2<WidgetConfigure> {

    private WidgetConfigure mActivity;

    private NumberPicker mHourPicker;

    private NumberPicker mMinutePicker;

    public WidgetConfigureTest() {
        super("fr.miximum.napply", WidgetConfigure.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        setActivityInitialTouchMode(false);

        mActivity = getActivity();

        mHourPicker = (NumberPicker) mActivity.findViewById(fr.miximum.napply.R.id.nap_hour);
        mMinutePicker = (NumberPicker) mActivity.findViewById(fr.miximum.napply.R.id.nap_minute);
    }

    public void testDurationPickers() {

        mHourPicker.setCurrent(0);
        mMinutePicker.setCurrent(10);
        assertEquals(10, mActivity.getNapDuration());

        mHourPicker.setCurrent(3);
        mMinutePicker.setCurrent(25);
        assertEquals(205, mActivity.getNapDuration());
    }
}
