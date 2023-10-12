package com.my.ssyx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//supplyAsync可以支持返回值。
public class Demo2 {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        System.out.println("main begin...");

        //CompletableFuture创建异步对象
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程："+Thread.currentThread().getName());
            int result= 1024;
            System.out.println("result:"+result);
            return result;
        }, executorService);
        Integer value = future.get();
        System.out.println("main over .... : "+value);
    }
}
