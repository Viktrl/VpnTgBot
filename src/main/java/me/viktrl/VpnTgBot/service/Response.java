package me.viktrl.VpnTgBot.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PUBLIC)
public class Response {
    int responseCode;
    String responseBody;
}