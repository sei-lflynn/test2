package gov.nasa.jpl.ammos.asec.kmc.api.sa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SpiGvcidTest {
    @Test
    public void testToString() throws JsonProcessingException {
        ObjectMapper mapper   = new ObjectMapper();
        SpiGvcid     spiGvcid = new SpiGvcid(1, (byte) 0, (short) 44, (byte) 1, (byte) 0);
        JsonNode     got      = mapper.readTree(spiGvcid.toString());
        JsonNode expect = mapper.createObjectNode()
                .put("spi", 1)
                .put("scid", 44)
                .put("vcid", 1)
                .put("mapid", 0)
                .put("tfvn", 0);

        assertTrue(expect.equals(got));
    }
}