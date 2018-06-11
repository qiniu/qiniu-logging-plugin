package com.qiniu.target.common;

/**
 * Created by jemy on 2018/6/11.
 */
public interface Analyzer {
    String StandardAnalyzer = "standard";
    String SimpleAnalyzer = "simple";
    String WhitespaceAnalyzer = "whitespace";
    String StopAnalyzer = "stop";
    String AnsjAnalyzer = "index_ansj";
    String DicAnajAnalyzer = "dic_ansj";
    String SearchAnsjAnalyzer = "search_ansj";
    String ToAnsjAnalyzer = "to_ansj";
    String UserAnsjAnalyzer = "user_ansj";
    String KeyWordAnalyzer = "keyword";
    String PathAnalyzer = "path";
}
