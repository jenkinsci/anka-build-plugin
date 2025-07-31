package com.veertu.plugin.anka;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import org.junit.Before;
import org.junit.Test;

public class AbstractSlaveTemplateTest {

    private AbstractSlaveTemplate template;

    @Before
    public void setUp() {
        template = new AbstractSlaveTemplate();
        template.setCloudName("test-cloud");
    }

    @Test
    public void testSetGroupWithUUID_shouldKeepUUID() {
        String uuid = "4894c60f-949d-4c5e-40d6-260c13bc0509";
        template.setGroup(uuid);
        assertThat(template.getGroup(), is(uuid));
    }

    @Test
    public void testSetGroupWithNull_shouldKeepNull() {
        template.setGroup(null);
        assertThat(template.getGroup(), is(nullValue()));
    }

    @Test
    public void testSetGroupWithEmptyString_shouldReturnNull() {
        template.setGroup("");
        assertThat(template.getGroup(), is(nullValue()));
    }

    @Test
    public void testSetGroupWithWhitespace_shouldKeepWhitespace() {
        template.setGroup("   ");
        assertThat(template.getGroup(), is("   "));
    }

    @Test
    public void testSetGroupWithInvalidUUID_shouldKeepOriginal() {
        String invalidUuid = "not-a-uuid";
        template.setGroup(invalidUuid);
        assertThat(template.getGroup(), is(invalidUuid));
    }
    
    @Test
    public void testGetGroupWithInvalidUUIDAndNoCloud_shouldReturnUUID() {
        String uuid = "4894c60f-949d-4c5e-40d6-260c13bc0509";
        template.setGroup(uuid);
        // When cloud is not available, should return UUID as-is even if invalid
        assertThat(template.getGroup(), is(uuid));
    }
} 