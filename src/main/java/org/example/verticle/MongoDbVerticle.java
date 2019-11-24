package org.example.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.example.data.Image;

import java.util.Base64;
import java.util.Objects;

public class MongoDbVerticle extends AbstractVerticle {
    private MongoClient client;
    @Override
    public void start() {
        client = MongoClient.createShared(vertx, new JsonObject()
                .put("db_name", "my_DB"));
        vertx.eventBus().consumer("database.save", this::saveDb);
        vertx.eventBus().consumer("getHistory", this::getHistory);
        vertx.eventBus().consumer("findImageById", this::findImageById);
        vertx.eventBus().consumer("findIdByImage", this::findIdByImage);
        vertx.eventBus().consumer("saveImage", this::saveImage);
        vertx.eventBus().consumer("dropCollection", this::dropCollection);
    }

    private void getHistory(Message<String> message) {
        client.find("message", new JsonObject(),
                result -> message.reply(Json.encode(result.result()))
        );
    }
    private void saveDb(Message<String> message) {
        client.insert("message", new JsonObject(message.body()), this::handler);
    }

    private void handler(AsyncResult<String> stringAsyncResult) {
        if (stringAsyncResult.succeeded()) {
            System.out.println("MongoDB save: " + stringAsyncResult.result());

        } else {
            System.out.println("ERROR MongoDB: " + stringAsyncResult.cause());
        }
    }

    private void findImageById(Message<String> message) {
        String id = message.body();
        if (id == null || id.isEmpty()) {
            message.reply(null);
            return;
        }

        client.find("images", new JsonObject().put("_id", id),
                result -> message.reply(result.result().stream().findAny().orElse(null)));
    }

    private void findIdByImage(Message<String> message) {
        Image image = Json.decodeValue(message.body(), Image.class);
        if (image == null || image.getImgData() == null) {
            message.reply(null);
            return;
        }

        image.fillHash();
        client.find("images", new JsonObject().put("hash", image.getHash()),
                result -> message.reply(result.result().stream()
                        .filter(json -> Objects.equals(
                                Base64.getEncoder().encodeToString(image.getImgData()), json.getString("imgData")))
                        .findAny().orElse(null)
                ));

    }

    /**
     * Сохраняет изображение в БД
     *
     * @param message изображение
     */
    private void saveImage(Message<String> message) {
        client.save("images", Json.decodeValue(message.body(), Image.class).toJsonObject(), result -> {
            if (result.succeeded()) {
                message.reply(result.result());
            } else {
                throw new RuntimeException(result.cause());
            }
        });
    }

    /**
     * Удаляет коллекцию из БД
     *
     * @param message название коллекции
     */
    private void dropCollection(Message<String> message) {
        client.dropCollection(message.body(), result -> {
            if (result.succeeded()) {
                message.reply(result.result());
            } else {
                throw new RuntimeException(result.cause());
            }
        });
    }
}
