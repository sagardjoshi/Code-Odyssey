package com.css.challenge.order.fulfilment.service;


import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import com.css.challenge.client.Temperature;
import com.css.challenge.order.fulfilment.storage.Shelf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OrderFulfilmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderFulfilmentService.class);
    private final Shelf hotShelf;
    private final Shelf coldShelf;
    private final Shelf roomShelf;

    private final Map<String, String> orderShelfMapping;
    private final List<Action> actionLog;

    public OrderFulfilmentService(int hotsize, int coldsize, int roomsize) {
        hotShelf = new Shelf(hotsize, Temperature.HOT);
        coldShelf = new Shelf(coldsize, Temperature.COLD);
        roomShelf = new Shelf(roomsize, Temperature.ROOM);
        orderShelfMapping = new HashMap<>();
        actionLog = new ArrayList<>();
    }

    public void placeOrder(Order order) {
        switch (order.getTemp()) {
            case Temperature.HOT:
                placeOrder(order, hotShelf);
                break;
            case Temperature.COLD:
                placeOrder(order, coldShelf);
                break;
            case Temperature.ROOM:
                placeRoomOrder(order);
                break;
            default:
                throw new RuntimeException("Invalid temperature");

        }
    }

    private void placeRoomOrder(Order order) {
        String orderId = order.getId();

        // Attempt to place the order on the room shelf
        if (roomShelf.put(orderId, order)) {
            // Successfully placed on the room shelf
            orderShelfMapping.put(orderId, Temperature.ROOM);
            actionLog.add(new Action(Instant.now(), orderId, Action.PLACE));
            return;
        }

        LOGGER.info("No space on the room shelf. Attempting to move eligible entries from the room shelf.");

        // Find an eligible order to move from the room shelf
        Order eligibleOrder = roomShelf.findEligibleOrder();

        if (eligibleOrder != null) {
            // Try to move the eligible order to its ideal shelf
            if (moveEligibleOrderToIdealShelf(eligibleOrder)) {
                // Place the new order on the room shelf after creating space
                roomShelf.put(orderId, order);
                orderShelfMapping.put(orderId, Temperature.ROOM);
                actionLog.add(new Action(Instant.now(), orderId, Action.PLACE));
            } else {
                // If unable to move the eligible order, evict an order and place the new one
                evictAndPlace(order);
            }
        } else {
            // If no eligible order is found, evict an order and place the new one
            evictAndPlace(order);
        }
    }

    private boolean moveEligibleOrderToIdealShelf(Order eligibleOrder) {
        String eligibleOrderId = eligibleOrder.getId();
        String orderTemp = eligibleOrder.getTemp();

        if (orderTemp.equalsIgnoreCase(Temperature.COLD) && coldShelf.hasSpace()) {
            moveOrder(eligibleOrderId, eligibleOrder, roomShelf, coldShelf, Temperature.COLD);
            return true;
        } else if (orderTemp.equalsIgnoreCase(Temperature.HOT) && hotShelf.hasSpace()) {
            moveOrder(eligibleOrderId, eligibleOrder, roomShelf, hotShelf, Temperature.HOT);
            return true;
        }
        return false;
    }

    private void moveOrder(String orderId, Order order, Shelf fromShelf, Shelf toShelf, String newTemp) {
        fromShelf.remove(orderId);
        toShelf.put(orderId, order);
        orderShelfMapping.put(orderId, newTemp);
        actionLog.add(new Action(Instant.now(), orderId, Action.MOVE));
    }

    private void evictAndPlace(Order order) {
        String orderId = order.getId();
        LOGGER.warn("There are no eligible orders on room shelf to move to hot or cold");
        String evictedOrderId = roomShelf.evictStaleOrder();
        if(!evictedOrderId.isEmpty()) {
            orderShelfMapping.remove(evictedOrderId);
            actionLog.add(new Action(Instant.now(), evictedOrderId, Action.DISCARD));

            roomShelf.put(orderId, order);
            orderShelfMapping.put(orderId, Temperature.ROOM);
            actionLog.add(new Action(Instant.now(), orderId, Action.PLACE));
        }
    }

    private void placeOrder(Order order, Shelf shelf) {
        //Try to place on ideal shelf
        String orderId = order.getId();
        if(!shelf.put(orderId, order)) {
            // Try to place on room temperature shelf
            LOGGER.info("No room on {} shelf trying on room shelf", order.getTemp());
            placeRoomOrder(order);
        } else {
            orderShelfMapping.put(orderId, order.getTemp());
            actionLog.add(new Action(Instant.now(), orderId, Action.PLACE));
        }
    }

    public void pickupOrder(String orderId) {
        if(!orderShelfMapping.containsKey(orderId)) {
            LOGGER.warn("The order {} is already discarded", orderId);
            return;
        }
        String temperature = orderShelfMapping.get(orderId);
        boolean pickUpStatus;
        switch (temperature) {
            case Temperature.COLD -> {
                pickUpStatus = coldShelf.remove(orderId);
            }
            case Temperature.HOT -> {
                pickUpStatus= hotShelf.remove(orderId);
            }
            case Temperature.ROOM -> {
                pickUpStatus = roomShelf.remove(orderId);
            }
            default -> throw new RuntimeException("Invalid temperature");
        }
        if(pickUpStatus) {
            actionLog.add(new Action(Instant.now(), orderId, Action.PICKUP));
            orderShelfMapping.remove(orderId);
        }
    }

    public List<Action> getActionLog() {
        return actionLog;
    }
}
