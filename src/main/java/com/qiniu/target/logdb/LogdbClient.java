package com.qiniu.target.logdb;


import com.qiniu.pandora.common.Config;
import com.qiniu.pandora.common.PandoraClient;
import com.qiniu.pandora.common.PandoraClientImpl;
import com.qiniu.pandora.common.QiniuException;
import com.qiniu.pandora.http.Client;
import com.qiniu.pandora.logdb.LogDBClient;
import com.qiniu.pandora.util.Auth;
import com.qiniu.pandora.util.Json;
import com.qiniu.pandora.util.StringMap;
import com.qiniu.target.common.Analyzer;
import com.qiniu.target.common.ValueType;

/**
 * Created by jemy on 2018/6/11.
 */
public class LogdbClient implements ValueType, Analyzer {
    private String logdbHost;
    private PandoraClient client;

    public LogdbClient(PandoraClient client) {
        this(client, "https://logdb.qiniu.com");
    }

    public LogdbClient(PandoraClient client, String logdbHost) {
        this.logdbHost = logdbHost;
        this.client = client;
    }

    /**
     * Create logdb repo
     */
    public void createRepo(String repoName, CreateRepoInput repoInput) throws Exception {
        if (repoInput.fullText != null && repoInput.fullText.enabled) {
            for (RepoSchemaEntry entry : repoInput.schema) {
                if (entry.valueType.equals(TypeString)) {
                    entry.analyzer = KeyWordAnalyzer;
                }
            }
        }

        String postUrl = String.format("%s/v5/repos/%s", this.logdbHost, repoName);
        String postBody = Json.encode(repoInput);
        this.client.post(postUrl, postBody.getBytes(Config.UTF_8), new StringMap(), Client.JsonMime);
    }

    /**
     * Check repo exists or not
     */
    public boolean repoExists(String repoName) {
        boolean exists = false;
        String getUrl = String.format("%s/v5/repos/%s", this.logdbHost, repoName);
        try {
            this.client.get(getUrl, new StringMap());
            exists = true;
        } catch (QiniuException e) {
            //pass
        }
        return exists;
    }

}


