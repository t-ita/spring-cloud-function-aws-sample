package com.myexample.serverless.aws;

import com.myexample.serverless.aws.functions.Greet;
import org.springframework.cloud.function.adapter.aws.SpringBootRequestHandler;

public class EventHandler extends SpringBootRequestHandler<Greet.Greeting, Greet.Greeting> {
}
