# qiniu-logging-plugin

## 背景

应用日志的收集和分析问题一直是服务开发过程中一块比较重要的部分。在行业上，简单的方式就是把日志写入到磁盘中，通过 awk，grep 等各种 shell 工具，使用复杂的过滤语句来实现日志的收集和分析。这种方式需要考虑日志文件的存储，日志的并发写入，日志文件按大小或者周期切换，定时删除旧的日志文件归档等等一系列的工作。

在 ELK 日志栈出现之后，大家开始流行搭建和维护自己的日志收集集群，通过 HTTP 或者其他协议的接口将应用的日志写入到集群中。在成熟的公司，会有专门的运维人员去维护这套日志的体系。但是在某些情况下，比如小公司或者测试情况下，可能也需要你自己搭建，自己维护等等，这个期间出现的各种复杂的问题不甚枚举。即使是成功搭建了 ELK 平台，在海量日志的侵蚀下，保持平台的稳定性也十分具有挑战性。

七牛云开发了一套完整的日志存储和分析平台，可以帮助客户实现海量日志数据的存储和分析。而本项目的目标就是在众多可选的日志收集方法中提供一种应用侵入性最低的方案。

当然，如果你对其他的日志收集方案感兴趣，我们还提供额外的工具[logkit](https://github.com/qiniu/logkit) 或者 [企业级logkit](https://logkit-pro.qiniu.com/#/)。

## 方案

基于 Java 应用大部分情况下都使用成熟的日志框架来做日志的收集工作，所以我们的基本思路就是为这些成熟的框架提供额外的 Appender 选项，作为一个插件加入到日志框架中。这样做的好处是，原有的 Java 应用不需要修改一行源码，不需要重新编译，只通过日志框架的配置文件的修改即可完成应用日志收集到七牛云大数据日志平台。
后续即可基于七牛云大数据日志平台做相关的日志分析等工作，整个流程非常简单，清晰，方便。

另外该方案在设计上面充分考虑实际生产环境下对上报性能的要求，在应用内部通过汇聚日志到内存缓冲区，然后批量上传。在日志量较大的情况，还会自动利用多线程后台上传。在网络异常的情况，还支持将日志临时缓存到本地磁盘，然后轮测网络，一旦恢复即可立即上传，上传成功还会删除临时缓存的文件，以节约磁盘空间等。

目前该项目方案已经可以支持 log4j 1.x，log4j 2.x 以及 logback。

## 下载

直接从[这里](http://devtools.qiniu.com/qiniu-logging-plugin-1.0.0.zip)下载。

或者可以通过 Maven 进行相关 Jar 的下载。

### log4j

```
<dependency>
    <groupId>com.qiniu</groupId>
    <artifactId>qiniu-logging-plugin-log4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

### log4j2

```
<dependency>
    <groupId>com.qiniu</groupId>
    <artifactId>qiniu-logging-plugin-log4j2</artifactId>
    <version>1.0.0</version>
</dependency>
```

### logback

```
<dependency>
    <groupId>com.qiniu</groupId>
    <artifactId>qiniu-logging-plugin-logback</artifactId>
    <version>1.0.0</version>
</dependency>
```

**备注** 每种日志框架请只使用对应的jar，一起使用可能会引起冲突，比如log4j和logback在使用slf4j的情况会冲突。

## 应用

本技术方案的应用十分简单。只需要三步即可完成。

1. 下载相关的jar并放入应用的 CLASSPATH 中；
2. 修改对应的 log4j 的配置文件，加入七牛 Appender 的相关配置；
3. 重启应用程序的进程。

相关配置模版可以参考如下文档：

1. [log4j 配置模版](log4j/src/test/resources/)
2. [log4j2 配置模版](log4j2/src/test/resources/)
3. [logback 配置模版](logback/src/test/resources/)

配置模版中各个参数的详细说明，请参考 [配置参数详解](docs/3.配置参数详解.md)

在模版配置中会有各个参数的设置情况，但是实际上，达到可用状态的配置最多只需要配置如下几个参数：

```
workflowName = logdemo1
pipelineRepo = logpipe1
logdbRepo = logdb1
accessKey = ak
secretKey = sk
```

其他的参数，一般情况下采用默认值即可，如果希望自行定义，请详细了解每个参数的含义然后进行设置。

## 实战

[ElasticSearch日志收集案例](https://github.com/jemygraw/qiniu-logging-plugin/wiki/ElasticSearch%E6%97%A5%E5%BF%97%E6%94%B6%E9%9B%86%E6%A1%88%E4%BE%8B)

[Apache Kafka日志收集案例](https://github.com/jemygraw/qiniu-logging-plugin/wiki/Apache-Kafka%E6%97%A5%E5%BF%97%E6%94%B6%E9%9B%86%E6%A1%88%E4%BE%8B)


## 反馈

如果有任何的反馈，希望您可以提交 ISSUE， 这样既可以记录问题，也可以给后面的人一个参考。

## 交流

如果有相关技术问题需要及时沟通，可以加入QQ群：675938456 。

