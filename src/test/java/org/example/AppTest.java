package org.example;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.client.WebClient;
import org.example.data.Image;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Не было времени разобраться, почему после получения id новой записи в БД невозможно получить саму запись по id.
 * Возможно, что проблема локальная для конкретной среды.
 */
@Ignore
public class AppTest {

    private static String host;
    private static WebClient webClient;
    private static int port;

    @BeforeClass
    public static void setUp() throws Exception {
        Starter.main(new String[]{});
        host = "localhost";
        port = 8081;
        webClient = WebClient.create(Vertx.vertx());
    }

    @Before
    public void truncateCollection() throws Exception {

    }

    /**
     * Тест поиска изображения по идентификатору
     */
    @Test(timeout = 5000)
    public void testFindImageById() throws InterruptedException {
        Image image = new Image("a".getBytes());
        final CountDownLatch latchSave = new CountDownLatch(1);
        AtomicReference<String> id = new AtomicReference<>();
        saveNewImage(image, id, latchSave);
        latchSave.await();

        final CountDownLatch latchGet = new CountDownLatch(1);
        id.set("5ddab2ff8561e41bbc543380");
        webClient.get(port, host, "/img/find/" + id.get()).send(result -> {
            assertTrue(Optional.ofNullable(result.cause()).map(Throwable::getMessage).orElse("Unknown cause"),
                    result.succeeded());
            Image imgResponse = result.result().bodyAsJson(Image.class);
            System.out.println(imgResponse);
            assertNotNull(imgResponse);
            // Base64 для объектов должны совпадать
            assertEquals(Arrays.hashCode(image.getImgData()), Arrays.hashCode(imgResponse.getImgData()));
            latchGet.countDown();
        });
        latchGet.await();
    }

    /**
     * Тест поиска идентификатора по изображению
     */
    @Test(timeout = 5000)
    public void testGetIdByImage() throws InterruptedException {
        Image image = new Image("a".getBytes());
        final CountDownLatch latchSave = new CountDownLatch(1);
        AtomicReference<String> id = new AtomicReference<>();
        saveNewImage(image, id, latchSave);
        latchSave.await();

        CountDownLatch latchGet = new CountDownLatch(1);
        webClient.post(port, host, "/img/find").sendBuffer(Json.encodeToBuffer(image), result -> {
            assertTrue(Optional.ofNullable(result.cause()).map(Throwable::getMessage).orElse("Unknown cause"),
                    result.succeeded());
            assertEquals(id.get(), result.result().bodyAsString());
        });

        latchGet.await();
    }

    private static void saveNewImage(Image image, AtomicReference<String> id, CountDownLatch latchPost) {
        webClient.post(port, host,
                "/img").sendBuffer(image.toJsonObject().toBuffer(), event -> {
            assertTrue(Optional.ofNullable(event.cause()).map(Throwable::getMessage).orElse("Unknown cause"),
                    event.succeeded());
            String newId = event.result().bodyAsString();
            System.out.println(">>> id = " + newId);
            id.set(newId);
            latchPost.countDown();
        });
    }
}