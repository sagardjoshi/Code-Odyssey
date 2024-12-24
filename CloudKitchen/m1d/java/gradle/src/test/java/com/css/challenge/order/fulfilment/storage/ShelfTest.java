package com.css.challenge.order.fulfilment.storage;

import com.css.challenge.client.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class ShelfTest {

    private Shelf shelf;

    @BeforeEach
    void setUp() {
        // Initialize a shelf with a size of 3 and type "COLD"
        shelf = new Shelf(3, "HOT");
    }

    @Test
    void testPutOrderSuccessfully() {
        // Create an order with temperature "COLD"
        Order order = new Order("order1",  "Pizza","HOT", 2);

        // Place the order on the shelf
        boolean isPlaced = shelf.put(order.getId(), order);

        // Assert the order was successfully placed
        assertTrue(isPlaced, "Order should be successfully placed on the shelf");
        assertTrue(shelf.hasSpace(), "Shelf should still have space after one order is placed");
    }

    @Test
    void testPutOrderWhenShelfFull() {
        // Fill the shelf to capacity
        shelf.put("order1", new Order("order1",  "Pizza","HOT", 2));
        shelf.put("order2", new Order("order1",  "Pizza","HOT", 2));
        shelf.put("order3", new Order("order1",  "Pizza","HOT", 2));

        // Attempt to add another order
        boolean isPlaced = shelf.put("order4", new Order("order1",  "Pizza","HOT", 2));

        // Assert the order was not placed
        assertFalse(isPlaced, "Order should not be placed when the shelf is full");
    }

    @Test
    void testRemoveOrderSuccessfully() {
        // Add an order to the shelf
        Order order = new Order("order1",  "Pizza","HOT", 2);
        shelf.put(order.getId(), order);

        // Remove the order
        boolean isRemoved = shelf.remove(order.getId());

        // Assert the order was successfully removed
        assertTrue(isRemoved, "Order should be successfully removed from the shelf");
        assertTrue(shelf.hasSpace(), "Shelf should have space after removing an order");
    }

    @Test
    void testRemoveNonExistentOrder() {
        // Attempt to remove an order that doesn't exist
        boolean isRemoved = shelf.remove("nonexistent_order");

        // Assert the removal failed
        assertFalse(isRemoved, "Removing a non-existent order should return false");
    }

    @Test
    void testFindEligibleOrder() {
        // Add orders to the shelf
        Order order1 = new Order("order1",  "Pizza","HOT", 2); // Non-ideal for a "COLD" shelf
        Order order2 = new Order("order2", "Pasta","HOT", 5); // Ideal
        shelf.put(order1.getId(), order1);
        shelf.put(order2.getId(), order2);

        // Find an eligible non-ideal order for eviction
        Order eligibleOrder = shelf.findEligibleOrder();

        // Assert the eligible order is correct
        assertNotNull(eligibleOrder, "Eligible order should be found");
        assertEquals("order1", eligibleOrder.getId(), "Eligible order should be the non-ideal order");
    }

    @Test
    void testFindEligibleOrderWhenNoneExist() {
        // Add only ideal orders to the shelf
        shelf.put("order1",new Order("order1",  "Pizza","HOT", 2));
        shelf.put("order2", new Order("order1",  "Pizza","HOT", 2));

        // Attempt to find an eligible non-ideal order
        Order eligibleOrder = shelf.findEligibleOrder();

        // Assert no eligible order is found
        assertNull(eligibleOrder, "No eligible order should be found when all orders are ideal");
    }

    @Test
    void testEvictOldestOrder() {
        // Add orders to the shelf
        Order order1 = new Order("order1",  "Pizza","HOT", 2);
        Order order2 = new Order("order1",  "Pizza","HOT", 2);
        Order order3 = new Order("order1",  "Pizza","HOT", 2);
        shelf.put(order1.getId(), order1);
        shelf.put(order2.getId(), order2);
        shelf.put(order3.getId(), order3);

        // Evict the oldest order
        String evictedOrderId = shelf.evictStaleOrder();

        // Assert the correct order was evicted
        assertEquals("order1", evictedOrderId, "The oldest order should be evicted");
        assertTrue(shelf.hasSpace(), "Shelf should have space after evicting an order");
    }

    @Test
    void testEvictOldestOrderWhenShelfEmpty() {
        // Attempt to evict an order from an empty shelf
        Exception exception = assertThrows(NoSuchElementException.class, () -> {
            shelf.evictStaleOrder();
        });

        // Assert an exception was thrown
        assertEquals("No value present", exception.getMessage(), "Exception should indicate no orders to evict");
    }
}
