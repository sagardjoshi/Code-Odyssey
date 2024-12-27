package com.css.challenge.order.fulfilment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.css.challenge.client.Action;
import com.css.challenge.client.Order;
import com.css.challenge.client.Temperature;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OrderFulfilmentServiceTest {

    private OrderFulfilmentService service;

    @BeforeEach
    public void setUp() {
        service = new OrderFulfilmentService(2, 2, 2);
    }

    @Test
    public void testPlaceOrder_HotShelf() {
        Order hotOrder = new Order("orderId1", "orderId1", Temperature.HOT, 10);
        service.placeOrder(hotOrder);

        List<Action> actionLog = service.getActionLog();
        assertEquals(1, actionLog.size());
        assertEquals(Action.PLACE, actionLog.get(0).getAction());
        assertEquals("orderId1", actionLog.get(0).getId());
        assertTrue(hotShelfContainsOrder(hotOrder.getId()));
    }

    @Test
    public void testPlaceOrder_ColdShelf() {
        Order coldOrder = new Order("orderId2", "orderId2", Temperature.COLD, 10);
        service.placeOrder(coldOrder);

        List<Action> actionLog = service.getActionLog();
        assertEquals(1, actionLog.size());
        assertEquals(Action.PLACE, actionLog.get(0).getAction());
        assertEquals("orderId2", actionLog.get(0).getId());
        assertTrue(coldShelfContainsOrder(coldOrder.getId()));
    }

    @Test
    public void testPlaceOrder_RoomShelf_SpaceAvailable() {
        Order roomOrder = new Order("orderId3", "orderId3",Temperature.ROOM, 10);
        service.placeOrder(roomOrder);

        List<Action> actionLog = service.getActionLog();
        assertEquals(1, actionLog.size());
        assertEquals(Action.PLACE, actionLog.get(0).getAction());
        assertEquals("orderId3", actionLog.get(0).getId());
        assertTrue(roomShelfContainsOrder(roomOrder.getId()));
    }

    @Test
    public void testPlaceOrder_RoomShelf_NoSpace_MoveEligibleToCold() throws InterruptedException {
        // Fill room shelf with cold orders
        service.placeOrder(new Order("orderId4", "orderId4", Temperature.COLD, 5));
        service.placeOrder(new Order("orderId5", "orderId5",Temperature.COLD, 10));


        service.placeOrder(new Order("orderId6", "orderId6",Temperature.COLD, 2));
        service.placeOrder(new Order("orderId7", "orderId7",Temperature.COLD, 10));
        service.pickupOrder("orderId4");

        Thread.sleep(1000L);
        service.placeOrder(new Order("orderId8", "orderId8",Temperature.HOT, 10));
        service.placeOrder(new Order("orderId9", "orderId9",Temperature.HOT, 10));


        service.placeOrder(new Order("orderId10", "orderId10",Temperature.HOT, 10));

        List<Action> actionLog = service.getActionLog();
        assertEquals(9, actionLog.size()); // Place actions for orderId4, orderId5, move of orderId4, place of orderId6

        // Check if orderId4 is moved to cold shelf and orderId6 is placed on room shelf
        assertTrue(coldShelfContainsOrder("orderId5"));
        assertTrue(roomShelfContainsOrder("orderId10"));
    }

    @Test
    public void testPlaceOrder_RoomShelf_NoSpace_Evict() throws InterruptedException {
        // Fill room shelf with orders
        service.placeOrder(new Order("orderId4", "orderId4", Temperature.COLD, 5));
        service.placeOrder(new Order("orderId5", "orderId5",Temperature.COLD, 10));

        service.placeOrder(new Order("orderId6", "orderId6",Temperature.COLD, 5));
        service.placeOrder(new Order("orderId7", "orderId7",Temperature.COLD, 10));
        Thread.sleep(2000L);

        service.placeOrder(new Order("orderId8", "orderId8",Temperature.HOT, 8));
        service.placeOrder(new Order("orderId9", "orderId9",Temperature.HOT, 3));

        service.placeOrder(new Order("orderId10", "orderId10",Temperature.HOT, 5));

        List<Action> actionLog = service.getActionLog();
        assertEquals(8, actionLog.size()); // Place actions for orderId10, orderId11, orderId12, evict, place of orderId13

        // Check if orderId12 is evicted and orderId13 is placed on room shelf
        assertFalse(roomShelfContainsOrder("orderId6"));
        assertEquals(Action.DISCARD, actionLog.get(6).getAction());
        assertEquals("orderId6", actionLog.get(6).getId());
    }

    @Test
    public void testPickupOrder_Success() {
        Order hotOrder = new Order("orderId14", "orderId14", Temperature.HOT, 10);
        service.placeOrder(hotOrder);
        service.pickupOrder("orderId14");

        List<Action> actionLog = service.getActionLog();
        assertEquals(2, actionLog.size());
        assertEquals(Action.PLACE, actionLog.get(0).getAction());
        assertEquals("orderId14", actionLog.get(0).getId());
        assertEquals(Action.PICKUP, actionLog.get(1).getAction());
        assertEquals("orderId14", actionLog.get(1).getId());
        assertFalse(hotShelfContainsOrder("orderId14"));
    }

    @Test
    public void testPickupOrder_OrderNotFound() {
        service.pickupOrder("orderId15");

        List<Action> actionLog = service.getActionLog();
        assertEquals(0, actionLog.size());
    }

    // Helper methods
    private boolean hotShelfContainsOrder(String orderId) {
        return service.getHotShelf().containsOrder(orderId);
    }

    private boolean coldShelfContainsOrder(String orderId) {
        return service.getColdShelf().containsOrder(orderId);
    }

    private boolean roomShelfContainsOrder(String orderId) {
        return service.getRoomShelf().containsOrder(orderId);
    }
}

