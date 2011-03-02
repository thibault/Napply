package fr.miximum.napply.tests;

import fr.miximum.napply.NapplyWidget;
import junit.framework.TestCase;

public class NapplyWidgetTest extends TestCase {

    public void testFormatNapDuration() {
        assertEquals("25", NapplyWidget.formatNapDuration(25));
        assertEquals("59", NapplyWidget.formatNapDuration(59));
        assertEquals("05", NapplyWidget.formatNapDuration(5));
        assertEquals("2:00", NapplyWidget.formatNapDuration(120));
        assertEquals("2:45", NapplyWidget.formatNapDuration(165));
    }
}
