package com.wanghui.kfc.kfcapi;

/** 上游返回错误状态或网络连接失败时的异常。 */
public class UpstreamException extends RuntimeException {
    /** 出错的上游服务。 */
    private final Upstream upstream;
    /** 上游状态码或连接失败时的 502。 */
    private final int status;

    /**
     * 创建上游调用异常。
     *
     * @param upstream 出错的上游
     * @param status HTTP 状态码，连接失败时为 502
     * @param message 可安全返回给本地调用方的错误说明
     */
    public UpstreamException(Upstream upstream, int status, String message) {
        super(message);
        this.upstream = upstream;
        this.status = status;
    }

    /**
     * 读取发生错误的上游。
     * @return 出错的上游
     */
    public Upstream getUpstream() { return upstream; }
    /**
     * 读取状态码。
     * @return 上游 HTTP 状态码，或连接失败时的 502
     */
    public int getStatus() { return status; }
}
