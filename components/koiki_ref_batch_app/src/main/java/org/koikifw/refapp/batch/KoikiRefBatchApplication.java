package org.koikifw.refapp.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KoikiRefBatchApplication {

    public static void main(String[] args) {
        // Propagate the KOIKI exit code (0/10/20/30) produced by the framework's
        // ExitCodeGenerator to the JVM process so schedulers such as JP1 can read it.
        System.exit(SpringApplication.exit(SpringApplication.run(KoikiRefBatchApplication.class, args)));
    }
}
