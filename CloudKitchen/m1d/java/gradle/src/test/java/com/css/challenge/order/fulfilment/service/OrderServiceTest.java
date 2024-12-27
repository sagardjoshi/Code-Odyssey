package com.css.challenge.order.fulfilment.service;

import com.css.challenge.client.Action;
import com.css.challenge.client.Client;
import com.css.challenge.client.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;



public class OrderServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceTest.class);
    private final Client client = new Client("https://api.cloudkitchens.com", "9znboe4344k7");
    
    public void testSlowKitchen() {
        int roomcapacity = 12;
        int coldcapacity = 6;
        int hotcapacity = 6;
        LOGGER.info("testSlowKitchen hot {}  cold {} room {} ", hotcapacity, coldcapacity, roomcapacity);
        processOrders(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofSeconds(8), hotcapacity, coldcapacity, roomcapacity);
    }

    public void testFastKitchen() {
        int roomcapacity = 12;
        int coldcapacity = 6;
        int hotcapacity = 6;
        LOGGER.info("testFastKitchen hot {}  cold {} room {} ", hotcapacity, coldcapacity, roomcapacity);
        processOrders(Duration.ofMillis(600), Duration.ofSeconds(2), Duration.ofSeconds(5), hotcapacity, coldcapacity, roomcapacity);
    }

    public void testHighCapacityShelves() {
        int roomcapacity = 16;
        int coldcapacity = 8;
        int hotcapacity = 8;
        LOGGER.info("testHighCapacityShelves hot {}  cold {} room {} ", hotcapacity, coldcapacity, roomcapacity);
        processOrders(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofSeconds(6), hotcapacity, coldcapacity, roomcapacity);
    }

    public void testLowCapacityShelves() {
        int roomcapacity = 8;
        int coldcapacity = 4;
        int hotcapacity = 4;
        LOGGER.info("testLowCapacityShelves hot {}  cold {} room {} ", hotcapacity, coldcapacity, roomcapacity);
        processOrders(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofSeconds(6), hotcapacity, coldcapacity, roomcapacity);
    }
    
    private void processOrders(Duration rate, Duration min, Duration max, int hotcapacity, int coldcapacity, int roomcapacity) {
        try {
            Problem problem = client.newProblem("", 0);

            LOGGER.info("Inverse Order Rate {} Pickup interval between {} and {}", rate, min, max);
            OrderFulfilmentService orderFulfilmentService = new OrderFulfilmentService(hotcapacity, coldcapacity, roomcapacity);
            OrderService orderService = new OrderService(rate.toMillis(), min.toMillis(), max.toMillis(), problem.getOrders(), orderFulfilmentService);
            orderService.startProcessing();
            orderService.waitForCompletion();
            orderService.stopProcessing();
            List<Action> actions = orderFulfilmentService.getActionLog();
            LOGGER.info("Actions performed {}", actions);
            String result = client.solveProblem(problem.getTestId(), rate, min, max, actions);
            LOGGER.info("Result: {}", result);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    public static void main(String[] args) {
        OrderServiceTest orderServiceTest = new OrderServiceTest();
        orderServiceTest.testSlowKitchen();
        orderServiceTest.testFastKitchen();
        orderServiceTest.testHighCapacityShelves();
        orderServiceTest.testLowCapacityShelves();
    }
}