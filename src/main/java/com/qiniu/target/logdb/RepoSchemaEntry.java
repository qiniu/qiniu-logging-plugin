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
}
