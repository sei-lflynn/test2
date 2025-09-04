package gov.nasa.jpl.ammos.asec.kmc.api.json;

import com.fasterxml.jackson.core.JsonParser;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ByteArrayDeserializerTest {

    @Test
    public void testDeserialize() throws IOException {
        JsonParser p = mock(JsonParser.class);
        when(p.getText()).thenReturn("01020304");
        ByteArrayDeserializer deserializer = new ByteArrayDeserializer();
        byte[]                ans          = deserializer.deserialize(p, null);
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04}, ans);
    }

}