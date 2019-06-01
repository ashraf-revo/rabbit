package org.revo.rabbit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.ReplayProcessor;

import java.time.Duration;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
@EnableBinding({Processor.class})
public class RabbitApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitApplication.class, args);
    }

    @Bean
    public FluxProcessor<Long, Long> processor() {
        return ReplayProcessor.create(0);
    }

    @Bean
    public Flux<Long> longFlux(FluxProcessor<Long, Long> processor) {
        return processor.publish().autoConnect();
    }

    @Bean
    public RouterFunction<ServerResponse> routerFunction(Flux<Long> longFlux) {
        return route(GET("/"), it -> ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(longFlux, Long.class));
    }

    @Autowired
    private FluxProcessor<Long, Long> processor;

    @StreamListener(Processor.INPUT)
    public void new_video(Message<Long> event) {
        processor.onNext(event.getPayload());
    }

    @Bean
    public CommandLineRunner runner(Processor processor) {
        return args -> {
            Flux.interval(Duration.ofSeconds(1))
                    .doOnEach(it -> processor.output().send(MessageBuilder.withPayload(it.get()).build()))
                    .subscribe();
        };
    }
}
