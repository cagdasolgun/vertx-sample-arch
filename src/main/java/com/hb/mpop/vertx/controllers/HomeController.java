package com.hb.mpop.vertx.controllers;

import javax.print.attribute.standard.PrinterLocation;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ContainerRequest;

import com.hb.mpop.vertx.feign.MyCustomService;

import feign.VertxFeign;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.Service;

@Path("/")
public class HomeController {

	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public void getString(@Suspended final AsyncResponse response, @Context ContainerRequest jerseyRequest,
			@Context HttpServerRequest vertxRequest, @Context Vertx vertx) {

		ConsulClient client = ConsulClient.create(vertx);

		client.catalogServiceNodes("myRestService", a -> {
			if (a.succeeded()) {
				System.out.println("found " + a.result().getList().size() + " services");

				Service service = a.result().getList().get(0);

				MyCustomService myCustomService = VertxFeign.builder().vertx(vertx).target(MyCustomService.class,
						"http://" + service.getAddress() + ":" + service.getPort());

				myCustomService.getSomething().setHandler(ar -> {
					if (ar.succeeded()) {
						response.resume(ar.result());
					} else {
						logger.error("remorte service call failed!");
					}
				});
			} else {
				a.cause().printStackTrace();
			}
		});

	}

}
