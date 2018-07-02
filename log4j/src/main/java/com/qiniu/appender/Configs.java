package com.qiniu.appender;


/**
 * Created by jemy on 2018/6/19.
 */
public interface Configs {
    String DefaultLogTag = "qiniu.plugin";
    int DefaultAutoFlushInterval = 5;//5 seconds
    String DefaultWorkflowRegion = "nb";//default is nb
    String DefaultLogdbRetention = "30d";//default is 30days
    String DefaultLogCacheDir = System.getProperty("java.io.tmpdir");
    int DefaultLogRotateFileSize = 2097152; //2MB
    int DefaultLogRotateInterval = 600; //10 minutes
    int DefaultLogRetryInterval = 60;//1 minute
}