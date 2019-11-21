package com.boot.netty.rpc.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.ZooKeeperServerListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@Component
@Slf4j
public class ServiceRegistry {

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    @Value("${registry.address}")
    private String registryAddress;


    private ZooKeeper connectServer(){
        ZooKeeper zook = null;

        try {
            zook = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if(event.getState() == Event.KeeperState.SyncConnected){
                        countDownLatch.countDown();
                    }
                }
            });
            countDownLatch.await();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return zook;
    }

    private void createRootNode(ZooKeeper zooKeeper){
        try {
            Stat s = zooKeeper.exists(Constant.ZK_REGISTRY_PATH, false);

            if(s == null){
                zooKeeper.create(Constant.ZK_REGISTRY_PATH,new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void register(String data){
        if(data != null){
            ZooKeeper zooKeeper = connectServer();
            if(zooKeeper != null){
                createRootNode(zooKeeper);

                createNode(zooKeeper,data);
            }
        }
    }


    public void createNode(ZooKeeper zk,String data){
        byte[] bytes = data.getBytes();
        try {
            String path = zk.create(Constant.ZK_DATA_PATH, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
