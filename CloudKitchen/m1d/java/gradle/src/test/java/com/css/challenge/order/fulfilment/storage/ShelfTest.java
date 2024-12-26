package com.css.challenge.order.fulfilment.storage;

import com.css.challenge.client.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ShelfTest {

    private Shelf hotShelf;
    private Shelf coldShelf;
    private Shelf roomShelf;
    private Order hotOrder;
    private Order coldOrder;
    private Order roomOrder;

    @BeforeEach
    void setUp() {
        hotShelf = new Shelf(1, "HOT");
        coldShelf = new Shelf(1, "COLD");
        roomShelf = new Shelf(2, "ROOM");

        hotOrder = new Order("1",  "Food1","HOT", 2);  // Freshness = 10 seconds
        coldOrder = new Order("2", "Food2","COLD", 5); // Freshness = 15 seconds
        roomOrder = new Order("3", "Food3", "ROOM", 7); // Freshness = 20 seconds
    }

    @Test
    void testPutAndHasSpace() {
        assertTrue(hotShelf.hasSpace());

        boolean added = hotShelf.put(hotOrder.getId(), hotOrder);
        assertTrue(added);
        assertFalse(hotShelf.hasSpace());

        added = coldShelf.put(coldOrder.getId(), coldOrder); // Non-ideal placement
        assertTrue(added);
        assertFalse(coldShelf.hasSpace());

        boolean notAdded = hotShelf.put(roomOrder.getId(), roomOrder); // No space
        assertFalse(notAdded);

        notAdded = coldShelf.put(roomOrder.getId(), roomOrder); // No space
        assertFalse(notAdded);
    }

    @Test
    void testRemove() {
        hotShelf.put(hotOrder.getId(), hotOrder);
        boolean removed = hotShelf.remove(hotOrder.getId());
        assertTrue(removed);

        boolean notRemoved = hotShelf.remove("nonexistent");
        assertFalse(notRemoved);
    }

    @Test
    void testFindEligibleOrder() throws InterruptedException {
        roomShelf.put(hotOrder.getId(), hotOrder);
        roomShelf.put(coldOrder.getId(), coldOrder);
        roomShelf.put(roomOrder.getId(), roomOrder);// Non-ideal placement

        Thread.sleep(3000);

        Order eligibleOrder = roomShelf.findEligibleOrder();
        assertNotNull(eligibleOrder);
        assertEquals(hotOrder.getId(), eligibleOrder.getId());
    }

    @Test
    void testEvictStaleOrder() throws InterruptedException {
        roomShelf.put(hotOrder.getId(), hotOrder);
        roomShelf.put(coldOrder.getId(), coldOrder);
        roomShelf.put(roomOrder.getId(), roomOrder);// Non-ideal placement


        Order eligibleOrder = roomShelf.findEligibleOrder();
        assertNull(eligibleOrder);

        String evictedOrderId = roomShelf.evictStaleOrder();
        assertEquals(coldOrder.getId(), evictedOrderId);
    }
}
