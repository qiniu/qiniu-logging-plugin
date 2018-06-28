package com.qiniu.tutorial;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

/**
 * Created by jemy on 2018/6/8.
 */
public class Log4j2 {
    @Test
    public void testLog4j2() {
        Logger logger = LogManager.getLogger("com.qiniu.log4j2");
        Marker SQL_MARKER = MarkerManager.getMarker("SQL");
        Marker UPDATE_MARKER = MarkerManager.getMarker("SQL_UPDATE").setParents(SQL_MARKER);
        Marker QUERY_MARKER = MarkerManager.getMarker("SQL_QUERY").setParents(SQL_MARKER);

        for(;;) {
            logger.trace("log4j2 trace level");
            logger.debug("log4j2 debug level");
            logger.info("log4j2 info level");
            logger.warn("log4j2 warn level");
            logger.error("log4j2 error level");
            logger.fatal("log4j2 fatal level", UPDATE_MARKER);
            try {
                FileInputStream fs = new FileInputStream(new File("xxx"));
            } catch (FileNotFoundException e) {
                //e.printStackTrace();
                logger.error("", "", new Throwable( e));
            }
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Test
    public void testException(){

        try {
            FileInputStream fs = new FileInputStream(new File("xxx"));
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
            System.out.println(e.getMessage());
            System.out.println(e.getLocalizedMessage());
        }
    }
}
