package org.cdpg.dx.deploy;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.cli.Option;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.JsonObject;
//import iudx.aaa.server.apiserver.util.ComposeException;
//import iudx.aaa.server.apiserver.util.ComposeExceptionMessageCodec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Deploys non-clustered vert.x instance of the server. As a JAR, the application requires 1 runtime
 * argument:
 *
 * <ul>
 *   <li>--config/-c : path to the config file
 * </ul>
 *
 * e.g. <i>java -jar ./fatjar.jar -c configs/config.json</i>
 */
public class DeployerDev {
  private static final Logger LOGGER = LogManager.getLogger(DeployerDev.class);

  public static void recursiveDeploy(Vertx vertx, JsonObject configs, int i) {
    if (i >= configs.getJsonArray("modules").size()) {
      LOGGER.info("Deployed all");
      return;
    }
    JsonObject config = configs.getJsonArray("modules").getJsonObject(i);
    String moduleName = config.getString("id");
    int numInstances = config.getInteger("verticleInstances");
    LOGGER.info("Deploying  ---> " + moduleName);
    vertx.deployVerticle(
        moduleName,
        new DeploymentOptions().setInstances(numInstances).setConfig(config),
        ar -> {
          if (ar.succeeded()) {
            LOGGER.info("Deployed {}", moduleName);
            recursiveDeploy(vertx, configs, i + 1);
          } else {
            LOGGER.fatal("Failed to deploy {}  cause : {}", moduleName, ar.cause());
          }
        });
  }

  public static void deploy(String configPath) {
    EventBusOptions ebOptions = new EventBusOptions();
    VertxOptions options = new VertxOptions().setEventBusOptions(ebOptions);

    String config;
    try {
      config = new String(Files.readAllBytes(Paths.get(configPath)), StandardCharsets.UTF_8);
    } catch (Exception e) {
      LOGGER.fatal("Couldn't read configuration file");
      return;
    }
    if (config.length() < 1) {
      LOGGER.fatal("Couldn't read configuration file");
      return;
    }
    JsonObject configuration = new JsonObject(config);
    try {
      ConfigResolve.resolve(configuration);
    } catch (IllegalStateException e) {
      LOGGER.fatal("Invalid option passed in config {}", e.getMessage());
      return;
    }
    try {
    Vertx vertx = Vertx.vertx(options);
    /*
     * Include ComposeException message codec so that ComposeException objects can be sent accross
     * the event bus
     */
    //TODO need to deafulat Codec
//    vertx
//        .eventBus();
//       // .registerDefaultCodec(ComposeException.class, new ComposeExceptionMessageCodec());
//    //LOGGER.debug("Added ComposeException message codec");
    recursiveDeploy(vertx, configuration, 0);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    CLI cli =
        CLI.create("IUDX Auth")
            .setSummary("A CLI to deploy the resource")
            .addOption(
                new Option()
                    .setLongName("help")
                    .setShortName("h")
                    .setFlag(true)
                    .setDescription("display help"))
            .addOption(
                new Option()
                    .setLongName("config")
                    .setShortName("c")
                    .setRequired(true)
                    .setDescription("configuration file"));

    LOGGER.debug("The logger has message lookups disabled ${LOG_LEVEL_PATTERN}");
    StringBuilder usageString = new StringBuilder();
    cli.usage(usageString);
    CommandLine commandLine = cli.parse(Arrays.asList(args), false);

    if (commandLine.isValid() && !commandLine.isFlagEnabled("help")) {
      String configPath = commandLine.getOptionValue("config");
      deploy(configPath);
    } else {
      LOGGER.info(usageString);
    }
  }
}
