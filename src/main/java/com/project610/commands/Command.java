package com.project610.commands;

import com.project610.Janna;
import com.project610.User;

import java.util.HashMap;
import java.util.function.Function;

import static com.project610.Janna.speechQueue;

public class Command implements Function {
    User user;
    String channel;
    String message;
    String[] split;
    Janna instance = Janna.instance;

    @Override
    public Object apply(Object o) {
        HashMap<String, Object> params = (HashMap<String, Object>)o;
        user = (User)params.get("user");
        message = "" + params.get("message");
        split = message.split(" ");
        channel = ""+params.get("channel");
        return this;
    }
}
