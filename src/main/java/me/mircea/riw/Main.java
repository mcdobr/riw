package me.mircea.riw;

import com.google.common.base.Stopwatch;
import me.mircea.riw.crawler.CrawlController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Instant;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("crawler -nt <<NUM_FETHCER_THREADS>> <<seed1>> <<seed2>> ... <<seedN>>");
            System.out.println("Make sure seeds end with /");
            System.exit(0);
        }

        LOGGER.info("Started at {}", Instant.now());

        switch (args[0].toLowerCase()) {
            case "crawl":
                int noThreads = 1;
                int seedStartIndex = 1;

                if (args[1].equalsIgnoreCase("-nt")) {
                    noThreads = Integer.parseInt(args[2]);
                    seedStartIndex = 3;
                }

                CrawlController controller = new CrawlController(noThreads, Paths.get("C:/Users/mircea/Desktop/dest/"));

                for (int seedIndex = seedStartIndex; seedIndex < args.length; ++seedIndex) {
                    try {
                        controller.add(new URI(args[seedIndex]));
                    } catch (URISyntaxException e) {
                        System.err.printf("URI %s was malformed!%n", args[seedIndex]);
                    }
                }

                try {

                    controller.add(new URI("http://riweb.tibeica.com/"));
                    controller.add(new URI("http://www.ace.tuiasi.ro/"));
                    controller.add(new URI("http://dmoz-odp.org/"));
                    controller.add(new URI("http://www.tcpipguide.com/"));
                    controller.add(new URI("http://www.zytrax.com/"));
                    controller.add(new URI("http://www.firewall.cx/"));

                    controller.crawl();
                } catch (URISyntaxException e) {
                    LOGGER.warn("A seed URI was malformed {}", e);
                } catch (InterruptedException e) {
                    LOGGER.error("A thread was interrupted {}", e);
                    Thread.currentThread().interrupt();
                }
        }

    }
}
