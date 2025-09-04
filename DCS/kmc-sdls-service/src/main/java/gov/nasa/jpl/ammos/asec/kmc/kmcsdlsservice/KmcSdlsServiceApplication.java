package gov.nasa.jpl.ammos.asec.kmc.kmcsdlsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KmcSdlsServiceApplication {

    public static void main(String[] args) {
        System.setProperty("spring.config.name","kmc-sdls-service");
        SpringApplication.run(KmcSdlsServiceApplication.class, args);
    }

}
