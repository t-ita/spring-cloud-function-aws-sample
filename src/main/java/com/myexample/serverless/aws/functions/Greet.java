package com.myexample.serverless.aws.functions;

import lombok.Data;

import java.util.function.Function;

public class Greet implements Function<Greet.Greeting, Greet.Greeting> {

    @Data
    public static class Greeting {
        String name;
        String message;
    }

    @Override
    public Greeting apply(Greeting greeting) {
        var res = new Greeting();
        res.name = "Spring Cloud Function";
        res.message = String.format("Hello, %s", greeting.name);
        return res;
    }
}
