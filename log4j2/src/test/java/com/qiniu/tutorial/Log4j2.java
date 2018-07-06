package com.qiniu.tutorial;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by jemy on 2018/6/8.
 */
public class Log4j2 {
    @Test
    public void testException() {
        Logger logger = LogManager.getLogger(Log4j2.class);
        try {
            FileInputStream fs = new FileInputStream(new File("xxx"));
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
            logger.error("this is an file not found exception message", new Throwable(e));
        }
        //wait for the flush
        try {
            TimeUnit.SECONDS.sleep(60);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testSlf4j() {
        Logger logger = LogManager.getLogger(Log4j2.class);
        int max = 10000;

        for (int index = 1; index <= max; index++) {
            logger.error(index + " this is an error message");
            try {
                TimeUnit.MILLISECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //wait for the flush
        try {
            TimeUnit.SECONDS.sleep(60);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMultiLogger() {
        int max = 10;
        Logger logger1 = LogManager.getLogger("logger1");
        Logger logger2 = LogManager.getLogger("logger2");
        List<Logger> loggerList = new ArrayList<>();
        loggerList.add(logger1);
        loggerList.add(logger2);

        for (Logger logger : loggerList) {
            for (int index = 1; index <= max; index++) {
                logger.error(index + " this is an error message");
                try {
                    TimeUnit.MILLISECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        //wait for the flush
        try {
            TimeUnit.SECONDS.sleep(60);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
