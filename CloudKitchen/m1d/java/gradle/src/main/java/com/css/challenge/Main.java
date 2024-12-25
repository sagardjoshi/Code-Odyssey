package com.css.challenge;

import com.css.challenge.client.Action;
import com.css.challenge.client.Client;
import com.css.challenge.client.Problem;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import com.css.challenge.order.fulfilment.service.OrderFulfilmentService;
import com.css.challenge.order.fulfilment.service.OrderService;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "challenge", showDefaultValues = true)
public class Main implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  static {
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT: %5$s %n");
  }

  @Option(names = "--endpoint", description = "Problem server endpoint")
  String endpoint = "https://api.cloudkitchens.com";

  @Option(names = "--auth", description = "Authentication token (required)")
  String auth = "9znboe4344k7";

  @Option(names = "--name", description = "Problem name. Leave blank (optional)")
  String name = "";

  @Option(names = "--seed", description = "Problem seed (random if zero)")
  long seed = 0;

  @Option(names = "--rate", description = "Inverse order rate")
  Duration rate = Duration.ofMillis(500);

  @Option(names = "--min", description = "Minimum pickup time")
  Duration min = Duration.ofSeconds(4);

  @Option(names = "--max", description = "Maximum pickup time")
  Duration max = Duration.ofSeconds(8);

  @Option(names = "--hotcapacity", description = "Hot Shelf Capacity")
  int hotcapacity = 6;

  @Option(names = "--coldcapacity", description = "Cold Shelf Capacity")
  int coldcapacity = 6;

  @Option(names = "--roomcapacity", description = "Room Shelf Capacity")
  int roomcapacity = 12;

  @Override
  public void run() {
    try {
      Client client = new Client(endpoint, auth);
      Problem problem = client.newProblem(name, seed);

      // ------ Simulation harness logic goes here using rate, min and max ----
      OrderFulfilmentService orderFulfilmentService = new OrderFulfilmentService(hotcapacity, coldcapacity, roomcapacity);
      OrderService orderService = new OrderService(rate.toMillis(), min.toMillis(), max.toMillis(), problem.getOrders(), orderFulfilmentService);
      orderService.startProcessing();
      orderService.waitForCompletion();
      orderService.stopProcessing();
      // ----------------------------------------------------------------------
      List<Action> actions = orderFulfilmentService.getActionLog();
      LOGGER.info("Actions performed {}", actions);

      String result = client.solveProblem(problem.getTestId(), rate, min, max, actions);

      LOGGER.info("Result: {}", result);

    } catch (IOException e) {
      LOGGER.error("Simulation failed: {}", e.getMessage());
    }
  }

  public static void main(String[] args) {
    new CommandLine(new Main()).execute(args);
  }
}
