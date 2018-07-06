package com.qiniu.tutorial;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by jemy on 2018/6/11.
 */
public class Slf4j {
    @Test
    public void testException() {
        Logger logger = LoggerFactory.getLogger(Slf4j.class);
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
        Logger logger = LoggerFactory.getLogger(Slf4j.class);
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
        Logger logger1 = LoggerFactory.getLogger("logger1");
        Logger logger2 = LoggerFactory.getLogger("logger2");
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
