package com.boot.netty.rpc.server;

import com.boot.netty.rpc.annotation.RpcService;
import com.boot.netty.rpc.protocal.RpcRequest;
import com.boot.netty.rpc.protocal.RpcResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.reflect.FastClass;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@Slf4j
public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private Map<String,Object> rpcMap;

    public RpcHandler(Map<String, Object> rpcMap) {
        this.rpcMap = rpcMap;
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
                RpcResponse response = new RpcResponse();

                response.setRequestId(request.getRequestId());

                try {
                    Object result = handle(request);
                    response.setResult(result);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        log.debug("send response for request " + request.getRequestId());
                    }
                });
            }
        });
    }

    private Object handle(RpcRequest request) throws InvocationTargetException {

        String className = request.getClassName();

        Object serviceBean = rpcMap.get(className);
        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] paramterTypes = request.getParamterTypes();
        Object[] paramters = request.getParamters();

        FastClass serviceFastClass = FastClass.create(serviceClass);

        int methodIndex = serviceFastClass.getIndex(methodName, paramterTypes);

        return serviceFastClass.invoke(methodIndex,serviceBean,paramters);


    }
}
