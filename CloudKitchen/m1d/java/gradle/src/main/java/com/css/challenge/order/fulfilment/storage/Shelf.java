package com.css.challenge.order.fulfilment.storage;

import com.css.challenge.client.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Shelf {
    private static final Logger LOGGER = LoggerFactory.getLogger(Shelf.class);
    private final HashMap<String, OrderWithTime> shelfMap;
    // Maintains a mapping between the order expiry time and orderId for faster eviction
    private final TreeMap<Long, String> orderSequenceMap;
    private final String shelfType;
    private final int capacity;
    // Used in case of Room Shelf to hold orderIds of hot and cold orders
    private final TreeMap<Long,String> nonIdealMap;

    private record OrderWithTime(Order order, Long timestamp) {
    }


    public Shelf(int size, String shelfType) {
        this.shelfMap = new HashMap<>(size);
        this.orderSequenceMap = new TreeMap<>();
        this.capacity = size;
        this.shelfType = shelfType;
        this.nonIdealMap = new TreeMap<>();
    }

    public synchronized boolean put(String key, Order value) {
        if(shelfMap.size() == capacity) {
            return false;
        }
        long expiryTimeStamp = System.currentTimeMillis() + (value.getFreshness() * 1000L);

        String temp = value.getTemp();
        if(!shelfType.equalsIgnoreCase(temp)) {
            nonIdealMap.put(expiryTimeStamp, key);
            expiryTimeStamp  /= 2; //if placed in ROOM shelf expiry reduces to half
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
            nonIdealMap.remove(orderWithTime.timestamp);
        }
        return true;
    }

    public synchronized boolean hasSpace() {
        return shelfMap.size() != capacity;
    }

    public synchronized Order findEligibleOrder() {
        if(nonIdealMap.isEmpty()) {
            return null;
        }
        Order eligibleOrder = null;
        long currentTimeStamp = System.currentTimeMillis();
        Map.Entry<Long, String> eligibleEntry = nonIdealMap.lowerEntry(currentTimeStamp); // Find the largest key < currentTimeStamp
        if (eligibleEntry != null) {
            eligibleOrder = shelfMap.get(eligibleEntry.getValue()).order; // Get the order associated with the eligible timestamp
        }
        return eligibleOrder;
    }

    public synchronized String evictStaleOrder() {
        long currentTimeStamp = System.currentTimeMillis();

        Map.Entry<Long, String> expiredEntry = orderSequenceMap.lowerEntry(currentTimeStamp); // Find the largest key < currentTimeStamp
        String orderId =  (expiredEntry != null) ? expiredEntry.getValue() : "";
        if(!orderId.isEmpty()) {
            OrderWithTime evictedRecord = shelfMap.remove(orderId);
            orderSequenceMap.remove(orderSequenceMap.firstKey());
            String temp = evictedRecord.order.getTemp();
            if (!shelfType.equalsIgnoreCase(temp)) {
                nonIdealMap.remove(evictedRecord.timestamp);
            }
        }
        return orderId;
    }

}
