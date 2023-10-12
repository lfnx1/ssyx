package com.my.ssyx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//串行话
public class Demo4 {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        //任务1 返回1024
        CompletableFuture<Integer> futureA = CompletableFuture.supplyAsync(() -> {
            int value= 1024;
            System.out.println("任务1："+value);
            return value;
        }, executorService);

        //任务2 获取任务1的返回结果
        CompletableFuture<Integer> futureB = futureA.thenApplyAsync((res) -> {
            System.out.println("任务2"+res);
            return res;
        }, executorService);

        //任务3  往下执行
        futureA.thenRunAsync(()->{
            System.out.println("任务3：");
        },executorService);

    }
}
