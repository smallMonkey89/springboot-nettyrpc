package com.boot.netty.rpc.server;

import com.boot.netty.rpc.annotation.RpcService;
import com.boot.netty.rpc.protocal.RpcRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {


    public RpcHandler(Map<String, Object> rpcMap) {
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("服务操作异常",cause);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        RpcServer.submit(new Runnable() {
            @Override
            public void run() {
                log.debug("receive request" + request.getRequestId());

            }
        });
    }
}
