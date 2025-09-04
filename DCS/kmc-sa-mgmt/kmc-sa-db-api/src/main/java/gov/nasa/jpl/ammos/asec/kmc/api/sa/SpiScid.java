package gov.nasa.jpl.ammos.asec.kmc.api.sa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

/**
 * SPI SCID identifier
 */
@Embeddable
public class SpiScid implements Serializable {
    private static final ObjectMapper mapper = new ObjectMapper();
    /**
     * SPI
     */
    private              Integer      spi;
    /**
     * SCID
     */
    private              Short        scid;

    /**
     * Constructor
     */
    public SpiScid() {

    }

    /**
     * Constructor
     *
     * @param spi  SPI
     * @param scid SCID
     */
    public SpiScid(Integer spi, Short scid) {
        this.spi = spi;
        this.scid = scid;
    }

    @Override
    public String toString() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get SPI
     *
     * @return SPI
     */
    public Integer getSpi() {
        return spi;
    }

    /**
     * Set SPI
     *
     * @param spi SPI
     */
    public void setSpi(Integer spi) {
        this.spi = spi;
    }

    /**
     * Get SCID
     *
     * @return SCID
     */
    public Short getScid() {
        return scid;
    }

    /**
     * Set SCID
     *
     * @param scid SCID
     */
    public void setScid(Short scid) {
        this.scid = scid;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + spi;
        result = 31 * result + scid;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof SpiScid)) {
            return false;
        }

        SpiScid id = (SpiScid) o;
        return this.spi.equals(id.spi) && this.scid.equals(id.scid);
    }
}
