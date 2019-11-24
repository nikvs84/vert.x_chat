package org.example.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.Optional;

public class RestServerVerticle extends AbstractVerticle {
    @Override
    public void start() {
        HttpServer httpServer = vertx.createHttpServer();
        Router httpRouter = Router.router(vertx);
        httpRouter.route().handler(BodyHandler.create());
        httpRouter.post("/sendMessage")
                .handler(request -> {
                    vertx.eventBus().send("router", request.getBodyAsString());
                    request.response().end("ok");
                });
        httpRouter.get("/getHistory")
                .handler(request ->
                        vertx.eventBus().send("getHistory", request.getBodyAsString(), result ->
                                request.response().end(result.result().body().toString())
                        )
                );
        httpRouter.get("/img/find/:id")
                .handler(request -> {
                    final String id = request.request().getParam("id");
                    vertx.eventBus().send("findImageById", id, result ->
                            request.response().end(Json.encodePrettily(Optional.ofNullable(result.result())
                                    .map(Message::body).orElse(null))));
                });
        httpRouter.post("/img/find/")
                .handler(request -> {
                    vertx.eventBus().send("findIdByImage", request.getBodyAsString(), result ->
                            request.response().end(Json.encodePrettily(Optional.ofNullable(result.result())
                                    .map(Message::body).orElse(null))));
                });
        httpRouter.post("/img")
                .handler(request -> {
                    vertx.eventBus().send("saveImage", request.getBodyAsString(), result ->
                            request.response().end(Json.encodePrettily(Optional.ofNullable(result.result())
                                    .map(Message::body)
                                    .orElseThrow(() -> new RuntimeException("Unexpected exception")))));
                });

        httpServer.requestHandler(httpRouter::accept);
        httpServer.listen(8081);
    }
}
