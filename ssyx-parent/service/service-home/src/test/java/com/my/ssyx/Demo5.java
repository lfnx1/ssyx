package com.my.ssyx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//组合任务
public class Demo5 {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        CompletableFuture<Integer> futureA = CompletableFuture.supplyAsync(() -> {
            System.out.println(Thread.currentThread().getName()+":begin..");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            int value=1024;
            System.out.println("任务1:" +value);
            System.out.println(Thread.currentThread().getName()+":end..");
            return value;
        }, executorService);

        CompletableFuture<Integer> futureB = CompletableFuture.supplyAsync(() -> {
            System.out.println(Thread.currentThread().getName()+":begin..");
            int value=200;
            System.out.println("任务2:" +value);
            System.out.println(Thread.currentThread().getName()+":end..");
            return value;
        }, executorService);

        CompletableFuture<Void> all = CompletableFuture.allOf(futureA, futureB);
        all.join();
        System.out.println("over.......");
    }
}
