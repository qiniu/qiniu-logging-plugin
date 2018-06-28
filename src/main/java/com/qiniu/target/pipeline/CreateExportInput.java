package com.qiniu.target.pipeline;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Created by jemy on 2018/6/13.
 */
public class CreateExportInput<T> {
    @SerializedName("type")
    public String type;
    @SerializedName("spec")
    public T spec;
    @SerializedName("whence")
    public String whence;
}

interface ExportSpec {

}

class LogdbSpec implements ExportSpec {
    @SerializedName("destRepoName")
    public String destRepoName;
    @SerializedName("doc")
    public Map<String, Object> doc;
    @SerializedName("omitInvalid")
    public boolean omitInvalid;
    @SerializedName("omitEmpty")
    public boolean omitEmpty;
}
