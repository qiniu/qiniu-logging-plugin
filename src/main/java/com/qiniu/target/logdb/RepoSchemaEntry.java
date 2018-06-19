package com.qiniu.target.logdb;

import com.google.gson.annotations.SerializedName;

/**
 * Created by jemy on 2018/6/11.
 */
public class RepoSchemaEntry {
    @SerializedName("key")
    public String key;
    @SerializedName("valtype")
    public String valueType;
    @SerializedName("analyzer")
    public String analyzer;
    @SerializedName("primary")
    public boolean primary;

    public RepoSchemaEntry(String key, String valueType) {
        this.key = key;
        this.valueType = valueType;
    }

    public RepoSchemaEntry(String key, String valueType, String analyzer) {
        this.key = key;
        this.valueType = valueType;
        this.analyzer = analyzer;
    }

    public RepoSchemaEntry(String key, String valueType, String analyzer, boolean primary) {
        this.key = key;
        this.valueType = valueType;
        this.analyzer = analyzer;
        this.primary = primary;
    }
}
