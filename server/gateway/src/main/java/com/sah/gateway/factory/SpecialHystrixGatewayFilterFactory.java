package com.sah.gateway.factory;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServiceUnavailableException;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Subscription;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

/**
 * @author yang.yonglian
 * @version 1.0.0
 * @Description TODO
 * @createTime 2022/1/10 15:19
 */
@Component
@Slf4j
public class SpecialHystrixGatewayFilterFactory extends AbstractGatewayFilterFactory<SpecialHystrixGatewayFilterFactory.Config> {

    private final ObjectProvider<DispatcherHandler> dispatcherHandlerProvider;

    // do not use this dispatcherHandler directly, use getDispatcherHandler() instead.
    private volatile DispatcherHandler dispatcherHandler;

    public SpecialHystrixGatewayFilterFactory(
            ObjectProvider<DispatcherHandler> dispatcherHandlerProvider) {
        super(SpecialHystrixGatewayFilterFactory.Config.class);
        this.dispatcherHandlerProvider = dispatcherHandlerProvider;
    }

    private DispatcherHandler getDispatcherHandler() {
        if (dispatcherHandler == null) {
            dispatcherHandler = dispatcherHandlerProvider.getIfAvailable();
        }

        return dispatcherHandler;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return singletonList(NAME_KEY);
    }

    @Override
    // TODO: make Config implement HasRouteId and remove this method.
    public GatewayFilter apply(String routeId, Consumer<Config> consumer) {
        SpecialHystrixGatewayFilterFactory.Config config = newConfig();
        consumer.accept(config);

        if (StringUtils.isEmpty(config.getName()) && !StringUtils.isEmpty(routeId)) {
            config.setName(routeId);
        }

        return apply(config);
    }

    /**
     * Create a {@link HystrixObservableCommand.Setter} based on incoming request attribute. <br>
     * This could be useful for example to create a Setter with {@link HystrixCommandKey}
     * being set as the target service's host:port, as obtained from
     * {@link ServerWebExchange#getRequest()} to do per service instance level circuit
     * breaking.
     */
    protected HystrixObservableCommand.Setter createCommandSetter(SpecialHystrixGatewayFilterFactory.Config config, ServerWebExchange exchange) {
        return config.setter;
    }

    @Override
    public GatewayFilter apply(SpecialHystrixGatewayFilterFactory.Config config) {
        // TODO: if no name is supplied, generate one from command id (useful for default
        // filter)
        if (config.setter == null) {
            Assert.notNull(config.name,
                    "A name must be supplied for the Hystrix Command Key");
            HystrixCommandGroupKey groupKey = HystrixCommandGroupKey.Factory
                    .asKey(getClass().getSimpleName());
            HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(config.name);

            config.setter = HystrixObservableCommand.Setter.withGroupKey(groupKey).andCommandKey(commandKey);
        }

        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange,
                                     GatewayFilterChain chain) {
                return Mono.deferWithContext(context -> {
                    ServerHttpRequest request = exchange.getRequest();
                    String path = request.getPath().pathWithinApplication().value();
                    Map<String, String> timeoutMap = config.getHystrix();
                    String hystrixName = null;
                    if (timeoutMap != null) {
                        //对rest接口通配符url进行转换 暂只配置url 末尾为数字的的接口---
                        path = config.wildCard(path);
                        hystrixName = timeoutMap.get(path);
                    }
                    HystrixObservableCommand.Setter setter;
                    if(hystrixName==null){
                        setter = createCommandSetter(config, exchange);
                    }else{
                        setter = config.hystrixCommandKeyMap.computeIfAbsent(hystrixName,t->{
                            HystrixCommandGroupKey groupKey = HystrixCommandGroupKey.Factory
                                    .asKey(SpecialHystrixGatewayFilterFactory.class.getSimpleName());
                            HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(t);
                            return HystrixObservableCommand.Setter.withGroupKey(groupKey).andCommandKey(commandKey);
                        });
                    }
                    SpecialHystrixGatewayFilterFactory.RouteHystrixCommand command = new SpecialHystrixGatewayFilterFactory.RouteHystrixCommand(
                            setter, config.fallbackUri,
                            exchange, chain, context);
                    return Mono.create(s -> {
                        Subscription sub = command.toObservable().subscribe(s::success,
                                s::error, s::success);
                        s.onCancel(sub::unsubscribe);
                    }).onErrorResume((Function<Throwable, Mono<Void>>) throwable -> {
                        if (throwable instanceof HystrixRuntimeException) {
                            HystrixRuntimeException e = (HystrixRuntimeException) throwable;
                            HystrixRuntimeException.FailureType failureType = e
                                    .getFailureType();

                            switch (failureType) {
                                case TIMEOUT:
                                    return Mono.error(new TimeoutException());
                                case SHORTCIRCUIT:
                                    return Mono.error(new ServiceUnavailableException());
                                case COMMAND_EXCEPTION: {
                                    Throwable cause = e.getCause();

                                    /*
                                     * We forsake here the null check for cause as
                                     * HystrixRuntimeException will always have a cause if the
                                     * failure type is COMMAND_EXCEPTION.
                                     */
                                    if (cause instanceof ResponseStatusException
                                            || AnnotatedElementUtils.findMergedAnnotation(
                                            cause.getClass(),
                                            ResponseStatus.class) != null) {
                                        return Mono.error(cause);
                                    }
                                }
                                default:
                                    break;
                            }
                        }
                        return Mono.error(throwable);
                    }).then();
                });
            }

            @Override
            public String toString() {
                return filterToStringCreator(SpecialHystrixGatewayFilterFactory.this)
                        .append("name", config.getName())
                        .append("fallback", config.fallbackUri).toString();
            }
        };
    }

    public static class Config {

        private String name;

        private HystrixObservableCommand.Setter setter;
        private ConcurrentHashMap<String,HystrixObservableCommand.Setter> hystrixCommandKeyMap = new ConcurrentHashMap();
        private URI fallbackUri;

        private Map<String, String> hystrix;

        public Map<String, String> getHystrix() {
            return hystrix;
        }

        public Config setHystrix(List<String> hystrix) {
            this.hystrix = hystrix.stream().map(str->str.split(":")).collect(Collectors.toMap(str->str[0],str->str[1]));
            return this;
        }

        public String getName() {
            return name;
        }

        public SpecialHystrixGatewayFilterFactory.Config setName(String name) {
            this.name = name;
            return this;
        }

        public SpecialHystrixGatewayFilterFactory.Config setFallbackUri(String fallbackUri) {
            if (fallbackUri != null) {
                setFallbackUri(URI.create(fallbackUri));
            }
            return this;
        }

        public URI getFallbackUri() {
            return fallbackUri;
        }

        public void setFallbackUri(URI fallbackUri) {
            if (fallbackUri != null && !"forward".equals(fallbackUri.getScheme())) {
                throw new IllegalArgumentException(
                        "Hystrix Filter currently only supports 'forward' URIs, found "
                                + fallbackUri);
            }
            this.fallbackUri = fallbackUri;
        }

        public SpecialHystrixGatewayFilterFactory.Config setSetter(HystrixObservableCommand.Setter setter) {
            this.setter = setter;
            return this;
        }

        public String wildCard(String path){
            String replace = path;
            String[] split = path.split("/");
            if (split.length>0) {
                String wildcard = split[split.length - 1];
                boolean numeric = isNumeric(wildcard);
                if (numeric) {
                    replace = path.replace(wildcard, "**");
                }
            }
            return replace;
        }
        private boolean isNumeric(String str) {
            String bigStr;
            try {
                bigStr = new BigDecimal(str).toString();
            } catch (Exception e) {
                return false;//异常 说明包含非数字。
            }
            return true;
        }


    }

    // TODO: replace with HystrixMonoCommand that we write
    private class RouteHystrixCommand extends HystrixObservableCommand<Void> {

        private final URI fallbackUri;

        private final ServerWebExchange exchange;

        private final GatewayFilterChain chain;

        private final Context context;

        RouteHystrixCommand(Setter setter, URI fallbackUri, ServerWebExchange exchange,
                            GatewayFilterChain chain, Context context) {
            super(setter);
            this.fallbackUri = fallbackUri;
            this.exchange = exchange;
            this.chain = chain;
            this.context = context;
        }

        @Override
        protected Observable<Void> construct() {
            return RxReactiveStreams
                    .toObservable(this.chain.filter(exchange).subscriberContext(context));
        }

        @Override
        protected Observable<Void> resumeWithFallback() {
            if (this.fallbackUri == null) {
                return super.resumeWithFallback();
            }

            // TODO: copied from RouteToRequestUrlFilter
            URI uri = exchange.getRequest().getURI();
            // TODO: assume always?
            boolean encoded = containsEncodedParts(uri);
            URI requestUrl = UriComponentsBuilder.fromUri(uri).host(null).port(null)
                    .uri(this.fallbackUri).scheme(null).build(encoded).toUri();
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
            addExceptionDetails();

            ServerHttpRequest request = this.exchange.getRequest().mutate()
                    .uri(requestUrl).build();
            ServerWebExchange mutated = exchange.mutate().request(request).build();
            // Before we continue on remove the already routed attribute since the
            // fallback may go back through the route handler if the fallback
            // is to another route in the Gateway
            removeAlreadyRouted(mutated);
            return RxReactiveStreams.toObservable(getDispatcherHandler().handle(mutated));
        }

        private void addExceptionDetails() {
            Throwable executionException = getExecutionException();
            ofNullable(executionException).ifPresent(exception -> exchange.getAttributes()
                    .put(HYSTRIX_EXECUTION_EXCEPTION_ATTR, exception));
        }

    }
}
