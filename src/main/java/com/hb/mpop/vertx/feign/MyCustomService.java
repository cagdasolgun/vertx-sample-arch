package com.hb.mpop.vertx.feign;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import feign.RequestLine;
import io.vertx.core.Future;

@Produces(MediaType.APPLICATION_JSON)
public interface MyCustomService {

	@RequestLine("GET /deneme")
	Future<String> getSomething();

}
