package com.qiniu.tutorial;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

/**
 * Created by jemy on 2018/6/11.
 */
public class Slf4j {
    @Test
    public void testSlf4j() {
        Logger logger = LoggerFactory.getLogger(Slf4j.class);

        while (true) {
            logger.trace("slf4j trace level 1");
            logger.debug("slf4j debug level 2");
            logger.info("slf4j info level 3");
            logger.warn("slf4j warn level 4");
            logger.error("slf4j error level 5");

            try {
                FileInputStream fs = new FileInputStream(new File("xxx"));
            } catch (FileNotFoundException e) {
                //e.printStackTrace();
                logger.error("exception", "o", new Throwable(e));
            }

            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
