package me.viktrl.VpnTgBot.service.POJOs;

import lombok.Data;

import java.util.Objects;

@Data
public class KeyResponse {
    String id;
    String name;
    String password;
    Integer port;
    String method;
    String accessUrl;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyResponse that = (KeyResponse) o;
        return id.equals(that.id) && Objects.equals(name, that.name) && Objects.equals(password, that.password) && Objects.equals(port, that.port) && Objects.equals(method, that.method) && Objects.equals(accessUrl, that.accessUrl);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(password);
        result = 31 * result + Objects.hashCode(port);
        result = 31 * result + Objects.hashCode(method);
        result = 31 * result + Objects.hashCode(accessUrl);
        return result;
    }


}
