/*
 * Copyright (c) 2011-2019, simple-http committers
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package simplehttp.listener;

import org.apache.log4j.Logger;
import simplehttp.HttpClient;
import simplehttp.HttpResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.MILLIS;

public class TimedHttpClient implements InvocationHandler {

    private final HttpClient delegate;
    private final Logger log;
    private final Clock clock;

    public static HttpClient timedHttpClient(HttpClient delegate, Clock clock, Class<?> logger) {
        verify(logger);
        return (HttpClient) Proxy.newProxyInstance(delegate.getClass().getClassLoader(),
                new Class[]{HttpClient.class}, new TimedHttpClient(delegate, clock, Logger.getLogger(logger)));
    }

    private static void verify(Class<?> logger) {
        if (logger.getName().equals(Object.class.getName()))
            throw new IllegalArgumentException("please carefully select a logger class and make sure it's configured in log4j.xml");
    }

    private TimedHttpClient(HttpClient delegate, Clock clock, Logger logger) {
        this.log = logger;
        this.clock = clock;
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Timer timer = new Timer(clock).start();
        Object result = null;
        try {
            result = method.invoke(delegate, args);
            return result;
        } finally {
            if (log.isInfoEnabled())
                log.info(message(method, args, timer, result));
        }
    }

    private String message(Method method, Object[] args, Timer timer, Object result) {
        return new StringBuilder()
            .append(method.getName().toUpperCase())
            .append(" ")
            .append(args[0].toString())
            .append(" was ")
            .append(response(result))
            .append(", took ")
            .append(timer.getElapsedTime().toMillis())
            .append("ms")
            .toString();
    }

    private static StringBuilder response(Object result) {
        StringBuilder builder = new StringBuilder();
        if (result instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) result;
            return builder.append(response.getStatusCode()).append(" (").append(response.getStatusMessage()).append(")");
        }
        return builder;
    }
    
    static class Timer {
        private final Clock clock;
        private long start = 0;

        Timer(Clock clock) {
            this.clock = clock;
        }

        Timer start() {
            start = clock.millis();    
            return this;
        }
        
        Duration getElapsedTime() {
            long now = clock.millis();
            if (now < start)
                throw new RuntimeException("please start the stop watch before stopping it");
            return Duration.of(now - start, MILLIS);
        }
    }
}
