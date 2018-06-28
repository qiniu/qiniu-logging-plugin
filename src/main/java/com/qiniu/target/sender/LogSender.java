package com.qiniu.target.sender;

import com.qiniu.pandora.common.PandoraClient;
import com.qiniu.pandora.common.QiniuException;
import com.qiniu.pandora.http.Response;
import com.qiniu.pandora.util.StringMap;

/**
 * Created by jemy on 2018/6/19.
 */
public class LogSender {
    private String pipelineHost;
    private String repoName;
    private PandoraClient client;

    public LogSender(String repoName, PandoraClient client) {
        this(repoName, client, "https://pipeline.qiniu.com");
    }

    public LogSender(String repoName, PandoraClient client, String pipelineHost) {
        this.pipelineHost = pipelineHost;
        this.client = client;
        this.repoName = repoName;
    }

    public String url(String repoName) {
        return String.format("%s/v2/repos/%s/data", this.pipelineHost, repoName);
    }

    public Response send(byte[] postBody) throws QiniuException {
        StringMap headers = new StringMap();
        String postUrl = this.url(repoName);
        return this.client.post(postUrl, postBody, headers, "text/plain");
    }
}
