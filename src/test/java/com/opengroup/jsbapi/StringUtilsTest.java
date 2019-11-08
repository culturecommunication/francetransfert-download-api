package com.opengroup.jsbapi;

import com.opengroup.jsbapi.domain.utils.StringUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilsTest {


    @Test
    public void testIsEmpty() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty(" "));
        assertFalse(StringUtils.isEmpty("foo"));
        assertFalse(StringUtils.isEmpty("  foo  "));
    }

    @Test
    public void testIsBlank() {
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank("   "));
        assertFalse(StringUtils.isBlank("foo"));
        assertFalse(StringUtils.isBlank("  foo  "));
    }


    @Test
    public void testTrim() {
        assertEquals("foo", StringUtils.trim(" foo "));
        assertNotEquals("  foo  ", StringUtils.trim("  foo  "));
    }


    @Test
    public void testRemove() {
        assertEquals("qeed", StringUtils.remove("queued", 'u'));
        assertNotEquals("qeed", StringUtils.remove("queued", 'z'));
    }

}
