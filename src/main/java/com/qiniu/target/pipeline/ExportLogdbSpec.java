package com.qiniu.target.pipeline;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Created by jemy on 2018/6/11.
 */
public class ExportLogdbSpec {
    @SerializedName("destRepoName")
    public String destRepoName;
    @SerializedName("doc")
    public Map<String, Object> doc;
    @SerializedName("omitInvalid")
    public boolean omitInvalid;
    @SerializedName("omitEmpty")
    public boolean omitEmpty;

    public ExportLogdbSpec(String destRepoName, Map<String, Object> doc, boolean omitInvalid, boolean omitEmpty) {
        this.destRepoName = destRepoName;
        this.doc = doc;
        this.omitInvalid = omitInvalid;
        this.omitEmpty = omitEmpty;
    }
}
