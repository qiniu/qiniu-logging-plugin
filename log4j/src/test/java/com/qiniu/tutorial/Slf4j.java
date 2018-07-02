package com.qiniu.tutorial;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by jemy on 2018/6/11.
 */
public class Slf4j {
    private int max = 10000;

    @Test
    public void testSlf4j() {
        Logger logger = LoggerFactory.getLogger(Slf4j.class);
        for (int i = 0; i < max; i++) {
            int rand = (int) (Math.random() * 3);
            if (rand == 0) {
                logger.info(i + " slf4j info level");
            } else if (rand == 1) {
                logger.warn(i + " slf4j warn level");
            } else if (rand == 2) {
                logger.error(i + " slf4j error level");
            } else {
                System.out.println(rand);
            }

            try {
                TimeUnit.MILLISECONDS.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testCount() {
        int sum = 0;
        File dir = new File("/Users/jemy/Temp/logging");
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.getName().contains(".log")) {
                System.out.println(f.getName());
                try {
                    BufferedReader fr = new BufferedReader(new FileReader(f));
                    String line = null;
                    try {
                        while ((line = fr.readLine()) != null) {
                            sum += 1;
                        }
                        fr.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("log count:" + sum);
        Assert.assertEquals(max, sum);
    }
}
