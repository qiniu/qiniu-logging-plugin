package com.qiniu.appender;


/**
 * Created by jemy on 2018/6/19.
 */
public interface Configs {
    String DefaultLogTag = "qiniu.plugin";
    // default log push auto flush interval
    int DefaultAutoFlushInterval = 5;
    // default workflow region
    String DefaultWorkflowRegion = "nb";
    //default logdb rentention
    String DefaultLogdbRetention = "30d";
    // default log cache dir
    String DefaultLogCacheDir = System.getProperty("java.io.tmpdir");
    // default log rotate file size
    int DefaultLogRotateFileSize = 2097152;
    // default log rotate interval in seconds
    int DefaultLogRotateInterval = 600;
    // default log retry interval in seconds
    int DefaultLogRetryInterval = 60;
    //log push thread pool size
    int DefaultLogPushThreadPoolSize = 20;
    // log push http request connect timeout in seconds
    int DefaultLogPushConnectTimeout = 10;
    // log push http request read timeout in seconds
    int DefaultLogPushReadTimeout = 30;
    // log push http request write timeout in seconds
    int DefaultLogPushWriteTimeout = 60;
    // log retry thread pool size
    int DefaultLogRetryThreadPoolSize = 10;
    // log retry http request connect timeout in seconds
    int DefaultLogRetryConnectTimeout = 10;
    // log retry http request read timeout in seconds
    int DefaultLogRetryReadTimeout = 30;
    // log retry http request write timeout in seconds
    int DefaultLogRetryWriteTimeout = 60;
}