package com.hb.mpop.vertx;

import java.util.Arrays;
import java.util.Collection;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.englishtown.vertx.hk2.HK2JerseyBinder;
import com.englishtown.vertx.hk2.HK2VertxBinder;
import com.englishtown.vertx.jersey.JerseyServer;
import com.hb.mpop.vertx.config.CustomBinder;

import io.atomix.AtomixClient;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.NettyTransport;
import io.atomix.vertx.AtomixClusterManager;
import io.atomix.vertx.ClusterSerializableSerializer;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.impl.ClusterSerializable;
import io.vertx.core.spi.cluster.ClusterManager;

public class Application {

	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		ConfigStoreOptions storeOptions = new ConfigStoreOptions().setType("consul")
				.setConfig(new JsonObject().put("host", "localhost").put("port", 8500).put("path", "vertx.conf"));

		// Build an Atomix client
		AtomixClient client = AtomixClient.builder().withTransport(NettyTransport.builder().withThreads(4).build())
				.build();

		// Register the Vert.x ClusterSerializable interface
		client.serializer().registerDefault(ClusterSerializable.class, ClusterSerializableSerializer.class);

		// Create a list of servers to which to connect
		Collection<Address> cluster = Arrays.asList(new Address("localhost", 8700), new Address("localhost", 8701),
				new Address("localhost", 8702));

		// Connect the client to the cluster
		client.connect(cluster).join();

		// Construct a Vert.x AtomixClusterManager
		ClusterManager clusterManager = new AtomixClusterManager(client);

		// Configure the Vert.x cluster manager and create a new clustered Vert.x
		// instance

		VertxOptions options = new VertxOptions().setClusterManager(clusterManager);
		Vertx.clusteredVertx(options, res -> {
			if (res.succeeded()) {
				Vertx vertx = res.result();

				ConfigRetriever retriever = ConfigRetriever.create(vertx,
						new ConfigRetrieverOptions().addStore(storeOptions));

				retriever.getConfig(ar -> {
					String map = ar.result().getMap().get("vertx.conf").toString();

					vertx.runOnContext(aVoid -> {

						// Set up the jersey configuration
						// The minimum config required is a package to inspect for JAX-RS endpoints
						vertx.getOrCreateContext().config().put("jersey",
								new JsonObject().put("port", new JsonObject(map).getInteger("port")).put("packages",
										new JsonArray().add("com.hb.mpop.vertx")));
						// Use a service locator (HK2 or Guice are supported by default) to create the
						// jersey server
						ServiceLocator locator = ServiceLocatorUtilities.bind(new HK2JerseyBinder(), new CustomBinder(),
								new HK2VertxBinder(vertx));
						JerseyServer server = locator.getService(JerseyServer.class);

						// Start the server which simply returns "Hello World!" to each GET request.
						server.start();
					});

				});

			} else {
				logger.error("Vertx context start failed.");
			}
		});
	}

}
