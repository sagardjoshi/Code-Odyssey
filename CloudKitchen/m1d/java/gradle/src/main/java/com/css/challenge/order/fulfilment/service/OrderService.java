package com.css.challenge.order.fulfilment.service;

import com.css.challenge.client.Order;
import com.css.challenge.configuration.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);
    private final long rate;
    private final long min;
    private final long max;
    private final List<Order> orderList;
    private final AtomicInteger placedCount;
    private final OrderFulfilmentService orderFulfilmentService;
    private final ScheduledExecutorService placeScheduler = Executors.newScheduledThreadPool(1);
    private final ScheduledExecutorService pickUpScheduler;
    private final CountDownLatch completionLatch;
    private final AppConfig appConfig;


    public OrderService(long rate, long min, long max, List<Order> orderList, OrderFulfilmentService orderFulfilmentService) {
        this.rate = rate;
        this.min = min;
        this.max = max;
        this.orderList = orderList;
        this.placedCount = new AtomicInteger(0);
        this.orderFulfilmentService = orderFulfilmentService;
        this.completionLatch = new CountDownLatch(orderList.size());
        this.appConfig = AppConfig.getInstance();
        String tpSize = appConfig.getProperty("thread.pool.size");
        int threadPoolSize = tpSize != null ? Integer.parseInt(tpSize) : 10;
        this.pickUpScheduler = Executors.newScheduledThreadPool(threadPoolSize);

    }

    public void startProcessing() {
        placeScheduler.scheduleAtFixedRate(this::placeOrder, 0L, this.rate, TimeUnit.MILLISECONDS);
    }

    public void waitForCompletion() {
        try {
            completionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Order processing was interrupted", e);
        }
    }

    public void stopProcessing() {
        placeScheduler.shutdown();
        pickUpScheduler.shutdown();
        try {
            if (!placeScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                placeScheduler.shutdownNow();
            }
            if (!pickUpScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                pickUpScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            placeScheduler.shutdownNow();
            pickUpScheduler.shutdownNow();
        }
    }

    private void placeOrder() {
        int currentCount = placedCount.getAndIncrement();
        if(currentCount >= orderList.size()) {
            placeScheduler.shutdown();
            return;
        }
        Order order = orderList.get(currentCount);
        orderFulfilmentService.placeOrder(order);

        long pickUpDelay = ThreadLocalRandom.current().nextLong(min, max + 1);
        LOGGER.debug("Picking up {} after {} ms", order.getId(), pickUpDelay);
        pickUpScheduler.schedule( () -> this.pickUpOrder(order.getId()) , pickUpDelay, TimeUnit.MILLISECONDS);

    }

    private void pickUpOrder(String orderId) {
        orderFulfilmentService.pickupOrder(orderId);
        completionLatch.countDown();
    }
}
