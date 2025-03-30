package me.viktrl.VpnTgBot.service.POJOs;

import java.util.Objects;

public class CreateKeyRequest {
    private String name;
//    String method;
//    String password;
//    Integer port;
//    CreateKeyRequestObjectDataLimit limit;


    public CreateKeyRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CreateKeyRequest that = (CreateKeyRequest) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "CreateKeyRequest{" +
                "name='" + name + '\'' +
                '}';
    }
}
