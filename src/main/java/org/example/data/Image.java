package org.example.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;

/**
 * Изображение
 */
public class Image {
    @JsonIgnore
    private String id;
    /**
     * Hash для изображения. Применяется для поиска идентификатора изображения в БД.
     */
    private int hash;
    /**
     * Изображение в виде массива байт
     */
    private byte[] imgData;

    public Image() {
    }

    public Image(byte[] imgData) {
        this.imgData = imgData;
        this.hash = Arrays.hashCode(imgData);
    }

    @JsonIgnore
    public String getId() {
        return id;
    }

    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }

    public byte[] getImgData() {
        return imgData;
    }

    public void setImgData(byte[] imgData) {
        this.imgData = imgData;
    }

    @Override
    public String toString() {
        getHash();
        return Json.encodePrettily(this);
    }

    /**
     * @return JsonObject без поля _id для сохранения в mongoDB
     */
    public JsonObject toJsonObject() {
        fillHash();
        JsonObject entries = JsonObject.mapFrom(this);
        return entries;
    }

    public void fillHash() {
        if (this.hash == 0 && this.imgData != null) {
            this.hash = Arrays.hashCode(this.imgData);
        }
    }
}
