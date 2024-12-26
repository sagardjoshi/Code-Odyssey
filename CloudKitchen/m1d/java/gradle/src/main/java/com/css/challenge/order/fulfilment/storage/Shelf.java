package com.css.challenge.order.fulfilment.storage;

import com.css.challenge.client.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.TreeMap;

public class Shelf {
    private static final Logger LOGGER = LoggerFactory.getLogger(Shelf.class);
    private final HashMap<String, OrderWithTime> shelfMap;
    // Maintains a mapping between the order expiry time and orderId for faster eviction
    private final TreeMap<Long, String> orderSequenceMap;
    // Used in case of Room Shelf to hold orderIds of hot and cold orders
    private final TreeMap<Long,String> nonIdealMapSequence;
    private final String shelfType;
    private final int capacity;


    private record OrderWithTime(Order order, Long timestamp) {
    }


    public Shelf(int size, String shelfType) {
        this.shelfMap = new HashMap<>(size);
        this.orderSequenceMap = new TreeMap<>();
        this.capacity = size;
        this.shelfType = shelfType;
        this.nonIdealMapSequence = new TreeMap<>();
    }

    public synchronized boolean put(String key, Order value) {
        if(shelfMap.size() == capacity) {
            return false;
        }
        long expiryTimeStamp = System.currentTimeMillis() + (value.getFreshness() * 1000L);

        String temp = value.getTemp();
        if(!shelfType.equalsIgnoreCase(temp)) {
            expiryTimeStamp  = System.currentTimeMillis() + ((value.getFreshness()/ 2) * 1000L); //if placed in ROOM shelf expiry reduces to half
            nonIdealMapSequence.put(expiryTimeStamp, key);
        }
        shelfMap.put(key, new OrderWithTime(value, expiryTimeStamp));
        orderSequenceMap.put(expiryTimeStamp, key);
        LOGGER.debug("Order {} Placed in {} Shelf ", key, temp);

        return true;
    }

    public synchronized boolean remove(String key) {
        if(!shelfMap.containsKey(key)) {
            return false;
        }

        OrderWithTime orderWithTime = shelfMap.remove(key);
        String temp = orderWithTime.order.getTemp();
        orderSequenceMap.remove(orderWithTime.timestamp);
        LOGGER.debug("Order {} Picked from {} Shelf ", key, temp);
        if(!shelfType.equalsIgnoreCase(temp)) {
            nonIdealMapSequence.remove(orderWithTime.timestamp);
        }
        return true;
    }

    public synchronized boolean hasSpace() {
        return shelfMap.size() != capacity;
    }

    public synchronized Order findEligibleOrder() {
        Order eligibleOrder = null;
        if(nonIdealMapSequence.isEmpty()) {
            return null;
        }

        long currentTimeStamp = System.currentTimeMillis();
        // check if expiry oldest non-ideal order < currentTimeStamp
        var eligibleEntry = nonIdealMapSequence.firstKey() < currentTimeStamp ? nonIdealMapSequence.firstEntry(): null;
        if (eligibleEntry != null) {
            eligibleOrder = shelfMap.get(eligibleEntry.getValue()).order; // Get the order associated with the eligible timestamp
        }
        return eligibleOrder;
    }

    public synchronized String evictStaleOrder() {
        long currentTimeStamp = System.currentTimeMillis();
        // check if expiry the oldest order < currentTimeStamp
        var expiredEntry = orderSequenceMap.firstKey() < currentTimeStamp ? orderSequenceMap.firstEntry(): null;
        String orderId =  (expiredEntry != null) ? expiredEntry.getValue() : "";
        if(!orderId.isEmpty()) {
            OrderWithTime evictedRecord = shelfMap.remove(orderId);
            orderSequenceMap.remove(expiredEntry.getKey());
            String temp = evictedRecord.order.getTemp();
            if (!shelfType.equalsIgnoreCase(temp)) {
                nonIdealMapSequence.remove(evictedRecord.timestamp);
            }
        }
        return orderId;
    }

    public synchronized boolean containsOrder(String key) {
        return shelfMap.containsKey(key);
    }
}
