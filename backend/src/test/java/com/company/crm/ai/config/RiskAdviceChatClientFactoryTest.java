package com.company.crm.ai.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

/** Only a synthetic local HTTP server; never a real Qwen request. */
class RiskAdviceChatClientFactoryTest {
    @Test void adviceHttpDeadlineAndNoRetryAreEffectiveForStalledBody() throws Exception {
        var server=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);
        var attempts=new AtomicInteger();
        var release=new CountDownLatch(1);
        server.createContext("/chat",exchange->{
            attempts.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            byte[] body="{\"id\":\"synthetic\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"synthetic\"},\"finish_reason\":\"stop\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type","application/json");
            exchange.sendResponseHeaders(200,body.length);
            try {
                if(release.await(2,TimeUnit.SECONDS)) exchange.getResponseBody().write(body);
            } catch(InterruptedException exception) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        var executor=Executors.newSingleThreadExecutor();
        server.setExecutor(executor); server.start();
        var timer=Executors.newSingleThreadScheduledExecutor();
        try {
            var factory=new RiskAdviceChatClientFactory("synthetic-test-key",
                "http://localhost:"+server.getAddress().getPort(),"/chat","synthetic");
            var client=factory.create(Duration.ofMillis(500));
            timer.schedule(release::countDown,2,TimeUnit.SECONDS);
            long started=System.nanoTime();
            assertThatThrownBy(()->client.prompt().user("synthetic").call().content())
                .isInstanceOf(RuntimeException.class);
            assertThat((System.nanoTime()-started)/1_000_000).isLessThan(1500);
            assertThat(attempts.get()).isEqualTo(1);
        } finally {release.countDown();server.stop(0);executor.shutdownNow();timer.shutdownNow();}
    }
}
