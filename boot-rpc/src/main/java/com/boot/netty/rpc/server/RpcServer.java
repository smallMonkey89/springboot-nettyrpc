package com.boot.netty.rpc.server;

import ch.qos.logback.classic.sift.AppenderFactoryUsingJoran;
import com.boot.netty.rpc.annotation.RpcService;
import com.boot.netty.rpc.protocal.RpcDecoder;
import com.boot.netty.rpc.protocal.RpcEncoder;
import com.boot.netty.rpc.protocal.RpcRequest;
import com.boot.netty.rpc.protocal.RpcResponse;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 服务端
 */
@Component
public class RpcServer implements ApplicationContextAware, InitializingBean {

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private ServiceRegistry serviceRegistry;

    private static ThreadPoolExecutor threadPoolExecutor;

    private NioEventLoopGroup bossGroup;

    private NioEventLoopGroup workgroup;

    private Map<String,Object> rpcMap = new HashMap<String, Object>();



    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }

    private void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workgroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();

        bootstrap.group(bossGroup,workgroup).channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE,true)
                .option(ChannelOption.SO_BACKLOG,128)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
                                .addLast(new RpcDecoder(RpcRequest.class))
                                .addLast(new RpcEncoder(RpcResponse.class))
                                .addLast(new RpcHandler(rpcMap));
                    }
                });

        String[] array = serverAddress.split(":");
        String host = array[0];
        int port = Integer.parseInt(array[1]);

        ChannelFuture future = bootstrap.bind(host, port).sync();

        if(serviceRegistry != null){
            serviceRegistry.register(serverAddress);
        }

        future.channel().closeFuture().sync();

    }

    public void stop(){
        if(bossGroup != null){
            bossGroup.shutdownGracefully();
        }

        if(workgroup != null){
            workgroup.shutdownGracefully();
        }
    }

    public static void submit(Runnable runnable){
        if(threadPoolExecutor == null){
            synchronized (RpcService.class){
                if(threadPoolExecutor == null){
                    threadPoolExecutor = new ThreadPoolExecutor(16,16,600L, TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(Integer.MAX_VALUE));
                }
            }
        }

        threadPoolExecutor.submit(runnable);
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(RpcService.class);

        if(MapUtils.isNotEmpty(serviceBeanMap)){
            for(Object serviceBean : serviceBeanMap.values()){
                String interfaceName = serviceBean.getClass().getAnnotation(RpcService.class).value();
                rpcMap.put(interfaceName,serviceBean);
            }
        }

    }
}
