/*******************************************************************************
 * Copyright (c) 2014, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 *   Patrick Tasse - Remove getSubField
 *******************************************************************************/
package org.eclipse.tracecompass.tmf.core.tests.trace.text;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.trace.text.TextTraceEventContent;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.text.SyslogEventType;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.text.SyslogEventType.Index;
import org.junit.Test;

/**
 * Test suite for the {@link TextTraceEventContent} class.
 */
@SuppressWarnings({ "javadoc", "nls" })
public class TextTraceEventContentTest {

    // ------------------------------------------------------------------------
    // Members
    // ------------------------------------------------------------------------

    private TextTraceEventContent fEventContent1;
    private TextTraceEventContent fEventContent1Clone;
    private TextTraceEventContent fEventContent2;
    private TextTraceEventContent fEventContent2Clone;

    public TextTraceEventContentTest () {
        fEventContent1 = new TextTraceEventContent(SyslogEventType.LABELS);
        fEventContent1.setValue("CONTENT");
        fEventContent1.setFieldValue(Index.TIMESTAMP, "Jan 1 01:01:01");
        fEventContent1.setFieldValue(Index.HOST, "HostA");
        fEventContent1.setFieldValue(Index.LOGGER, "LoggerA");
        fEventContent1.setFieldValue(Index.FILE, "SourceFileA");
        fEventContent1.setFieldValue(Index.LINE, "0");
        fEventContent1.setFieldValue(Index.MESSAGE, "MessageA");

        fEventContent1Clone = new TextTraceEventContent(SyslogEventType.LABELS);
        fEventContent1Clone.setValue("CONTENT");
        fEventContent1Clone.setFieldValue(Index.TIMESTAMP, "Jan 1 01:01:01");
        fEventContent1Clone.setFieldValue(Index.HOST, "HostA");
        fEventContent1Clone.setFieldValue(Index.LOGGER, "LoggerA");
        fEventContent1Clone.setFieldValue(Index.FILE, "SourceFileA");
        fEventContent1Clone.setFieldValue(Index.LINE, "0");
        fEventContent1Clone.setFieldValue(Index.MESSAGE, "MessageA");

        fEventContent2 = new TextTraceEventContent(SyslogEventType.LABELS);
        fEventContent2.setFieldValue(SyslogEventType.LABELS[0], "Jan 1 02:02:02");
        fEventContent2.setFieldValue(SyslogEventType.LABELS[1], "HostB");
        fEventContent2.setFieldValue(SyslogEventType.LABELS[2], "LoggerB");
        fEventContent2.setFieldValue(SyslogEventType.LABELS[3], "SourceFileB");
        fEventContent2.setFieldValue(SyslogEventType.LABELS[4], "2");
        StringBuffer buffer = new StringBuffer();
        buffer.append("Message B");
        fEventContent2.setFieldValue(SyslogEventType.LABELS[5],   buffer);

        fEventContent2Clone = new TextTraceEventContent(SyslogEventType.LABELS);
        fEventContent2Clone.setFieldValue(SyslogEventType.LABELS[0], "Jan 1 02:02:02");
        fEventContent2Clone.setFieldValue(SyslogEventType.LABELS[1], "HostB");
        fEventContent2Clone.setFieldValue(SyslogEventType.LABELS[2], "LoggerB");
        fEventContent2Clone.setFieldValue(SyslogEventType.LABELS[3], "SourceFileB");
        fEventContent2Clone.setFieldValue(SyslogEventType.LABELS[4], "2");
        buffer = new StringBuffer();
        buffer.append("Message B");
        fEventContent2Clone.setFieldValue(SyslogEventType.LABELS[5],   buffer);
    }

    public void testConstructorConstructor() {
        assertEquals("getField:TIMESTAMP", "Jan 1 01:01:01", fEventContent1.getFieldValue(Index.TIMESTAMP));
        assertEquals("getField:HOST", "HostA", fEventContent1.getFieldValue(Index.HOST));
        assertEquals("getField:LOGGER", "LoggerA", fEventContent1.getFieldValue(Index.LOGGER));
        assertEquals("getField:FILE", "SourceFileA", fEventContent1.getFieldValue(Index.FILE));
        assertEquals("getField:LINE", "0", fEventContent1.getFieldValue(Index.LINE));
        assertEquals("getField:MESSAGE", "MessageA", fEventContent1.getFieldValue(Index.MESSAGE).toString());
    }

    // ------------------------------------------------------------------------
    // Event Type
    // ------------------------------------------------------------------------

    @Test
    public void testEventTypeInstance() {
        SyslogEventType eventType = SyslogEventType.INSTANCE;
        assertEquals("getTypeId", "Syslog", eventType.getName());
        assertNotNull ("instance", eventType);
        assertTrue (eventType.getFieldNames().contains("Timestamp"));
        assertTrue (eventType.getFieldNames().contains("Host"));
        assertTrue (eventType.getFieldNames().contains("Logger"));
        assertTrue (eventType.getFieldNames().contains("File"));
        assertTrue (eventType.getFieldNames().contains("Line"));
        assertTrue (eventType.getFieldNames().contains("Message"));
    }

    // ------------------------------------------------------------------------
    // equals
    // ------------------------------------------------------------------------

    @Test
    public void testEquals() {
        assertEquals("equals", fEventContent1, fEventContent1);
        assertEquals("equals", fEventContent2, fEventContent2);

        assertTrue("equals", !fEventContent1.equals(fEventContent2));
        assertTrue("equals", !fEventContent2.equals(fEventContent1));

        assertEquals("equals", fEventContent1, fEventContent1Clone);
        assertEquals("equals", fEventContent2, fEventContent2Clone);
    }

    @Test
    public void testEqualsNull() {
        assertTrue("equals", !fEventContent1.equals(null));
        assertTrue("equals", !fEventContent2.equals(null));
    }

    // ------------------------------------------------------------------------
    // hashCode
    // ------------------------------------------------------------------------

    @Test
    public void testHashCode() {

        assertEquals("hashCode", fEventContent1.hashCode(), fEventContent1Clone.hashCode());
        assertEquals("hashCode", fEventContent2.hashCode(), fEventContent2Clone.hashCode());

        assertNotEquals("hashCode", fEventContent1.hashCode(), fEventContent2.hashCode());
        assertNotEquals("hashCode", fEventContent2.hashCode(), fEventContent1.hashCode());
    }

    // ------------------------------------------------------------------------
    // Event Content
    // ------------------------------------------------------------------------

    @Test
    public void testGetFieldValueWithIndex() {
        assertEquals("getFieldValue:TIMESTAMP", "Jan 1 01:01:01", fEventContent1.getFieldValue(Index.TIMESTAMP));
        assertEquals("getFieldValue:HOST", "HostA", fEventContent1.getFieldValue(Index.HOST));
        assertEquals("getFieldValue:LOGGER", "LoggerA", fEventContent1.getFieldValue(Index.LOGGER));
        assertEquals("getFieldValue:FILE", "SourceFileA", fEventContent1.getFieldValue(Index.FILE));
        assertEquals("getFieldValue:LINE", "0", fEventContent1.getFieldValue(Index.LINE));
        assertEquals("getFieldValue:MESSAGE", "MessageA", fEventContent1.getFieldValue(Index.MESSAGE));
        assertNull(fEventContent1.getFieldValue(6));
    }

    @Test
    public void testGetFieldValueWithName() {
        assertEquals("getFieldValue:TIMESTAMP", "Jan 1 01:01:01", fEventContent1.getFieldValue("Timestamp"));
        assertEquals("getFieldValue:HOST", "HostA", fEventContent1.getFieldValue("Host"));
        assertEquals("getFieldValue:LOGGER", "LoggerA", fEventContent1.getFieldValue("Logger"));
        assertEquals("getFieldValue:FILE", "SourceFileA", fEventContent1.getFieldValue("File"));
        assertEquals("getFieldValue:LINE", "0", fEventContent1.getFieldValue("Line"));
        assertEquals("getFieldValue:MESSAGE", "MessageA", fEventContent1.getFieldValue("Message"));
        assertNull(fEventContent1.getFieldValue("BlaBla"));
    }

    @Test
    public void testGetFieldNameWithIndex() {

        assertEquals("getFieldName:TIMESTAMP", SyslogEventType.LABELS[0], fEventContent1.getFieldName(Index.TIMESTAMP));
        assertEquals("getFieldName:HOST", SyslogEventType.LABELS[1], fEventContent1.getFieldName(Index.HOST));
        assertEquals("getFieldName:LOGGER", SyslogEventType.LABELS[2], fEventContent1.getFieldName(Index.LOGGER));
        assertEquals("getFieldName:FILE", SyslogEventType.LABELS[3], fEventContent1.getFieldName(Index.FILE));
        assertEquals("getFieldName:LINE", SyslogEventType.LABELS[4], fEventContent1.getFieldName(Index.LINE));
        assertEquals("getFieldName:MESSAGE", SyslogEventType.LABELS[5], fEventContent1.getFieldName(Index.MESSAGE));
        assertNull(fEventContent1.getFieldValue(6));
    }

    @Test
    public void testGetFields() {
        List<TextTraceEventContent> fields = fEventContent1.getFields();
        assertEquals(6, fields.size());
        assertEquals("getFields:TIMESTAMP", SyslogEventType.LABELS[0], fields.get(Index.TIMESTAMP).getName());
        assertEquals("getFields:TIMESTAMP", "Jan 1 01:01:01", fields.get(Index.TIMESTAMP).getValue());
        assertEquals("getFields:HOST", SyslogEventType.LABELS[1], fields.get(Index.HOST).getName());
        assertEquals("getFields:HOST", "HostA", fields.get(Index.HOST).getValue());
        assertEquals("getFields:LOGGER", SyslogEventType.LABELS[2], fields.get(Index.LOGGER).getName());
        assertEquals("getFields:LOGGER", "LoggerA", fields.get(Index.LOGGER).getValue());
        assertEquals("getFields:FILE", SyslogEventType.LABELS[3], fields.get(Index.FILE).getName());
        assertEquals("getFields:FILE", "SourceFileA", fields.get(Index.FILE).getValue());
        assertEquals("getFields:LINE", SyslogEventType.LABELS[4], fields.get(Index.LINE).getName());
        assertEquals("getFields:LINE", "0", fields.get(Index.LINE).getValue());
        assertEquals("getFields:MESSAGE", SyslogEventType.LABELS[5], fields.get(Index.MESSAGE).getName());
        assertEquals("getFields:MESSAGE", "MessageA", fields.get(Index.MESSAGE).getValue());
    }

    @Test
    public void testGetFieldWithName() {
        ITmfEventField field = fEventContent1.getField("Timestamp");
        assertEquals("getFieldName:TIMESTAMP", SyslogEventType.LABELS[0], field.getName());
        assertEquals("getFieldName:TIMESTAMP", "Jan 1 01:01:01", field.getValue());

        field = fEventContent1.getField(SyslogEventType.LABELS[1]);
        assertEquals("getFieldName:HOST", SyslogEventType.LABELS[1], field.getName());
        assertEquals("getFieldName:HOST", "HostA", field.getValue());

        field = fEventContent1.getField(SyslogEventType.LABELS[2]);
        assertEquals("getFieldName:LOGGER", SyslogEventType.LABELS[2], field.getName());
        assertEquals("getFieldName:LOGGER", "LoggerA", field.getValue());

        field = fEventContent1.getField(SyslogEventType.LABELS[3]);
        assertEquals("getFieldName:FILE", SyslogEventType.LABELS[3], field.getName());
        assertEquals("getFieldName:FILE", "SourceFileA", field.getValue());

        field = fEventContent1.getField(SyslogEventType.LABELS[4]);
        assertEquals("getFieldName:LINE", SyslogEventType.LABELS[4], field.getName());
        assertEquals("getFieldName:LINE", "0", field.getValue());

        field = fEventContent1.getField(SyslogEventType.LABELS[5]);
        assertEquals("getFieldName:Message", SyslogEventType.LABELS[5], field.getName());
        assertEquals("getFieldName:Message", "MessageA", field.getValue());

        field = fEventContent1.getField("BlaBla");
        assertNull(field);
    }

    @Test
    public void testGetFieldWithPath() {
        String[] path = { "Timestamp" };

        ITmfEventField field = fEventContent1.getField(path);
        assertEquals("getFieldPath:TIMESTAMP", SyslogEventType.LABELS[0], field.getName());
        assertEquals("getFieldPath:TIMESTAMP", "Jan 1 01:01:01", field.getValue());

        String[] path2 = { "Timestamp", "subField" };
        field = fEventContent1.getField(path2);
        assertNull(field);
    }

    @Test
    public void testGetFormattedValue() {
        assertEquals("CONTENT", fEventContent1.getFormattedValue());
    }

    @Test
    public void testToString() {
        assertEquals("Timestamp=Jan 1 01:01:01, Host=HostA, Logger=LoggerA, File=SourceFileA, Line=0, Message=MessageA", fEventContent1.toString());
    }

    @Test
    public void testGetFieldNames() {
        String[] labels = {"Timestamp", "Host", "Logger", "File", "Line", "Message"};
        List<String> names = fEventContent1.getFieldNames();
        assertArrayEquals(labels, names.toArray(new String[names.size()]));
    }

}
