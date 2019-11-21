package com.boot.netty.rpc;

import org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchTest {

    public static CountDownLatch latch = new CountDownLatch(10);

    public static void main(String[] args) throws InterruptedException {
        for (int i=0;i<10;i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println(Thread.currentThread().getName());
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        System.out.println("main---------------");
    }
}
