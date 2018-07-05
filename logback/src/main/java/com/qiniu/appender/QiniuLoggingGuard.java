package com.qiniu.appender;

import com.qiniu.pandora.common.Constants;
import com.qiniu.pandora.pipeline.sender.DataSender;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by jemy on 2018/7/2.
 */
public class QiniuLoggingGuard {
    private static QiniuLoggingGuard instance;
    private DataSender dataSender;
    private FilenameFilter logFileFilter;
    private String logTag;
    private String logCacheDir;
    private int logRotateInterval; // in seconds
    private int logRetryInterval; // in seconds

    private Map<String, Boolean> retryingFiles;
    private ExecutorService retryService;
    private int currentLogCounter;
    private String currentLogFileName;
    private File currentLogFile;
    private long currentFileLength;
    private Calendar currentLogCalendar;
    private BufferedOutputStream currentLogWriter;
    private long logRotateIntervalInMillis;
    private long logRotateIntervalInMinutes;

    private QiniuLoggingGuard() {
        this.logTag = Configs.DefaultLogTag;
        this.logCacheDir = Configs.DefaultLogCacheDir;
        this.logRotateInterval = Configs.DefaultLogRotateInterval;
        this.logRetryInterval = Configs.DefaultLogRetryInterval;

        this.logRotateIntervalInMillis = this.logRotateInterval * 1000;
        this.logRotateIntervalInMinutes = this.logRotateInterval / 60;
        this.retryingFiles = new ConcurrentHashMap<String, Boolean>();
        this.currentFileLength = 0;
        this.currentLogCounter = 0;

        this.logFileFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(logTag);
            }
        };

        //create the backend task to put logs
        new Thread(new Runnable() {
            @Override
            public void run() {
                pushLogs();
            }
        }).start();
    }

    private void pushLogs() {
        while (true) {
            File logCacheDirFile = new File(this.logCacheDir);
            if (logCacheDirFile.exists()) {
                //filter all the log file
                File[] logFiles = logCacheDirFile.listFiles(this.logFileFilter);
                if (logFiles != null) {
                    for (File logFile : logFiles) {
                        if (logFile.getName().endsWith(".log.tmp")) {
                            //check file last modified
                            Date lastModifiedDate = new Date(logFile.lastModified());
                            //add log rotate time
                            Calendar rotateDate = Calendar.getInstance();
                            rotateDate.setTime(lastModifiedDate);
                            if (rotateDate.getTimeInMillis() < System.currentTimeMillis()) {
                                //change it to log file with suffix .log
                                String logFilePath = logFile.getAbsolutePath();
                                File dstLogFile = new File(logFilePath.substring(0, logFilePath.length() - 4));
                                logFile.renameTo(dstLogFile);
                            }
                        }
                    }
                }

                //rescan the log files and upload
                File[] readyLogFiles = logCacheDirFile.listFiles(this.logFileFilter);
                if (readyLogFiles != null) {
                    for (final File logFile : readyLogFiles) {
                        if (logFile.getName().endsWith(".log")) {
                            final String logFileAbsPath = logFile.getAbsolutePath();
                            //check whether the last upload finishes
                            if (this.retryingFiles.containsKey(logFileAbsPath)) {
                                //wait for next scheduled time
                                continue;
                            }
                            this.retryingFiles.put(logFileAbsPath, true);
                            //try to upload the data
                            retryService.execute(new Runnable() {
                                @Override
                                public void run() {
                                    byte[] logBuffer = new byte[(int) logFile.length()];
                                    FileInputStream fs = null;
                                    try {
                                        fs = new FileInputStream(logFile);
                                        fs.read(logBuffer);
                                        dataSender.send(logBuffer);
                                        //delete the file when sent success
                                        logFile.delete();
                                    } catch (FileNotFoundException e) {
                                        //e.printStackTrace();
                                    } catch (IOException e) {
                                        //e.printStackTrace();
                                    } finally {
                                        if (fs != null) {
                                            try {
                                                fs.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                    //whether success or not, delete key
                                    retryingFiles.remove(logFileAbsPath);
                                }
                            });
                        }
                    }
                }
            }
            //wait for the next run
            try {
                TimeUnit.SECONDS.sleep(this.logRetryInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static QiniuLoggingGuard getInstance(int logRetryThreadPoolSize) {
        if (instance == null) {
            synchronized (QiniuLoggingGuard.class) {
                if (instance == null) {
                    instance = new QiniuLoggingGuard();
                    instance.retryService = Executors.newFixedThreadPool(logRetryThreadPoolSize);
                }
            }
        }
        return instance;
    }

    public void setDataSender(DataSender dataSender) {
        this.dataSender = dataSender;
    }

    public void setLogCacheDir(String logCacheDir) throws FileNotFoundException {
        this.logCacheDir = logCacheDir;
        File cacheDir = new File(this.logCacheDir);
        if (!cacheDir.exists() || !cacheDir.isDirectory() || !cacheDir.canWrite()) {
            throw new FileNotFoundException(this.logCacheDir + " must exists and be a writable directory");
        }
    }

    public void setLogRotateInterval(int logRotateInterval) {
        this.logRotateInterval = logRotateInterval;
        this.logRotateIntervalInMillis = this.logRotateInterval * 1000;
        this.logRotateIntervalInMinutes = this.logRotateInterval / 60;
    }

    public void setLogRetryInterval(int logRetryInterval) {
        this.logRetryInterval = logRetryInterval;
    }

    public synchronized void write(byte[] logBuffer) {
        int logBufferLen = logBuffer.length;
        //check rotate interval
        long currentTime = System.currentTimeMillis();
        Date currentDate = new Date(currentTime);
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTime(currentDate);

        long currentLogMinute = currentCalendar.get(Calendar.MINUTE) / this.logRotateIntervalInMinutes *
                this.logRotateIntervalInMinutes;
        String currentLogFilePrefix = String.format("%04d_%02d_%02d_%02d_%02d", currentCalendar.get(Calendar.YEAR),
                currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.DAY_OF_MONTH),
                currentCalendar.get(Calendar.HOUR_OF_DAY), currentLogMinute);

        if (this.currentLogWriter == null) {
            //read from local disk when app restarted
            while (true) {
                //skip the old files counter
                String oldLogFileName = String.format("%s_%010d.%s.log", currentLogFilePrefix, this.currentLogCounter,
                        this.logTag);
                File oldLogFile = new File(this.logCacheDir, oldLogFileName);
                if (oldLogFile.exists()) {
                    this.currentLogCounter += 1;
                    continue;
                }

                break;//found a new log file
            }

            this.currentLogFileName = String.format("%s_%010d.%s.log.tmp", currentLogFilePrefix,
                    this.currentLogCounter, this.logTag);
            this.currentLogFile = new File(this.logCacheDir, this.currentLogFileName);
            boolean createNewLog = false;
            if (currentLogFile.exists()) {
                if (logBufferLen + this.currentLogFile.length() < Configs.DefaultLogRotateFileSize) {
                    try {
                        this.currentLogWriter = new BufferedOutputStream(new FileOutputStream(this.currentLogFile,
                                true));
                        this.currentFileLength = this.currentLogFile.length();
                        this.currentLogCalendar = currentCalendar;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        //NOTE never happens
                    }
                } else {
                    this.currentLogCounter += 1;
                    createNewLog = true;
                }
            } else {
                createNewLog = true;
            }

            if (createNewLog) {
                //rename the old file
                String currentLogFilePath = this.currentLogFile.getAbsolutePath();
                File dstLogFile = new File(currentLogFilePath.substring(0, currentLogFilePath.length() - 4));
                this.currentLogFile.renameTo(dstLogFile);

                //create new file
                this.currentLogFileName = String.format("%s_%010d.%s.log.tmp", currentLogFilePrefix,
                        this.currentLogCounter, this.logTag);
                this.currentLogFile = new File(this.logCacheDir, this.currentLogFileName);

                try {
                    this.currentLogWriter = new BufferedOutputStream(new FileOutputStream(this.currentLogFile));
                    this.currentFileLength = 0;
                    this.currentLogCalendar = currentCalendar;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    //NOTE never happens
                }
            }
        }

        //when log writer not null, check the file size before write the log
        //when log writer not null, check the log rotate before write the log
        boolean rotateBySize = logBufferLen + this.currentFileLength > Configs.DefaultLogRotateFileSize;
        boolean rotateByInterval = currentCalendar.getTimeInMillis() - this.currentLogCalendar.getTimeInMillis()
                > this.logRotateIntervalInMillis;
        if (rotateBySize || rotateByInterval) {
            if (rotateByInterval) {
                this.currentLogCounter = 0;
            } else {
                this.currentLogCounter += 1;
            }

            //close the old writer and create a new one
            try {
                this.currentLogWriter.flush();
                this.currentLogWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
                //NOTE ignore
            }

            //change tmp log file to finished log file
            String currentLogFilePath = this.currentLogFile.getAbsolutePath();
            File dstLogFile = new File(currentLogFilePath.substring(0, currentLogFilePath.length() - 4));
            this.currentLogFile.renameTo(dstLogFile);

            //create new log file
            this.currentLogFileName = String.format("%s_%010d.%s.log.tmp", currentLogFilePrefix,
                    this.currentLogCounter, this.logTag);
            this.currentLogFile = new File(this.logCacheDir, this.currentLogFileName);
            try {
                this.currentLogWriter = new BufferedOutputStream(new FileOutputStream(this.currentLogFile));
                this.currentFileLength = 0;
                this.currentLogCalendar = currentCalendar;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                //NOTE ignore
            }
        }

        //write log buffer to file
        try {
            this.currentLogWriter.write(logBuffer);
            this.currentLogWriter.flush();
            this.currentFileLength += logBufferLen;
        } catch (IOException e) {
            e.printStackTrace();
            //NOTE write error, output the log buffer to console
            System.out.println(new String(logBuffer, Constants.UTF_8));
        }
    }

    public synchronized void close() {
        try {
            if (this.currentLogWriter != null) {
                this.currentLogWriter.flush();
                this.currentLogWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
