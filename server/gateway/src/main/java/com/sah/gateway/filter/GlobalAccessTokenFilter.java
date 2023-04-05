package com.sah.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.sah.gateway.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;

/**
 *
 */
@Slf4j
@Component
public class GlobalAccessTokenFilter implements GlobalFilter, Ordered {
    public final static String X_ACCESS_TOKEN = "X-Access-Token";
    public final static String REFRESH_X_ACCESS_TOKEN = "Refresh-X-Access-Token";
    public final static String X_GATEWAY_BASE_PATH = "X_GATEWAY_BASE_PATH";

//    @Autowired
//    private CommonApi commonApi;
    @Value("${excludeUrls}")
    private String excludeUrls;
    @Value("${requestParamVerifyTokenUrl}")
    private String requestParamVerifyTokenUrl;
    private PathMatcher pathMatcher = new AntPathMatcher();

    private List<String> excludeUrlList = new ArrayList<>();

    private List<String> requestParamVerifyTokenUrls = new ArrayList<>();

    @PostConstruct
    public void init(){
        Arrays.stream(excludeUrls.split(",")).forEach(excludeUrlList::add);
        excludeUrlList.add("/sys/cas/client/validateLogin");
        excludeUrlList.add("/sys/randomImage/**");
        excludeUrlList.add("/sys/checkCaptcha");
        excludeUrlList.add("/sys/login");
        excludeUrlList.add("/sys/mLogin");
        excludeUrlList.add("/sys/logout");
        excludeUrlList.add("/sys/dict/getDictItems/**");
        excludeUrlList.add("/sys/thirdLogin/**");
        excludeUrlList.add("/sys/getEncryptedString");
        excludeUrlList.add("/sys/sms");
        excludeUrlList.add("/sys/phoneLogin");
        excludeUrlList.add("/sys/user/checkOnlyUser");
        excludeUrlList.add("/sys/user/register");
        excludeUrlList.add("/sys/user/querySysUser");
        excludeUrlList.add("/sys/user/phoneVerification");
        excludeUrlList.add("/sys/user/passwordChange");
        excludeUrlList.add("/auth/2step-code");
        excludeUrlList.add("/sys/common/static/**");
        excludeUrlList.add("/sys/common/pdf/**");
        excludeUrlList.add("/**/getLastSysVersion");
        excludeUrlList.add("/generic/**");
        excludeUrlList.add("/");
        excludeUrlList.add("/doc.html");
        excludeUrlList.add("/**/*.js");
        excludeUrlList.add("/**/*.css");
        excludeUrlList.add("/**/*.html");
        excludeUrlList.add("/**/*.svg");
        excludeUrlList.add("/**/*.pdf");
        excludeUrlList.add("/**/*.jpg");
        excludeUrlList.add("/**/*.png");
        excludeUrlList.add("/**/*.ico");
        excludeUrlList.add("/**/*.ttf");
        excludeUrlList.add("/**/*.woff");
        excludeUrlList.add("/**/*.woff2");
        excludeUrlList.add("/druid/**");
        excludeUrlList.add("/swagger-ui.html");
        excludeUrlList.add("/swagger**/**");
        excludeUrlList.add("/webjars/**");
        excludeUrlList.add( "/v2/**");
        excludeUrlList.add("/jmreport/**");
        excludeUrlList.add("/**/*.js.map");
        excludeUrlList.add("/**/*.css.map");
        excludeUrlList.add("/bigscreen/**");
        excludeUrlList.add("/test/bigScreen/**");
        excludeUrlList.add("/websocket/**");
        excludeUrlList.add("/newsWebsocket/**");
        excludeUrlList.add("/monitorData/**");
        excludeUrlList.add("/backstage/**");
        excludeUrlList.add("/vxeSocket/**");
        excludeUrlList.add("/eoaSocket/**");
        excludeUrlList.add("/actuator/**");
        Arrays.stream(requestParamVerifyTokenUrl.split(",")).forEach(requestParamVerifyTokenUrls::add);
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        try{
            String url = exchange.getRequest().getURI().getPath();
            String scheme = exchange.getRequest().getURI().getScheme();
            String host = exchange.getRequest().getURI().getHost();
            int port = exchange.getRequest().getURI().getPort();
            String basePath = scheme + "://" + host + ":" + port;
//        log.info(" base path :  "+ basePath);

            // 1. 重写StripPrefix(获取真实的URL)
            addOriginalRequestUrl(exchange, exchange.getRequest().getURI());
            String rawPath = exchange.getRequest().getURI().getRawPath();
            String newPath = "/" + Arrays.stream(StringUtils.tokenizeToStringArray(rawPath, "/")).skip(1L).collect(Collectors.joining("/"));
            ServerHttpRequest newRequest = exchange.getRequest().mutate().path(newPath).build();
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, newRequest.getURI());

            //将现在的request，添加当前身份
            ServerHttpRequest mutableReq = exchange.getRequest().mutate().header("Authorization-UserName", "").header(X_GATEWAY_BASE_PATH, basePath).build();
            ServerWebExchange mutableExchange = exchange.mutate().request(mutableReq).build();
            ServerHttpResponse resp = exchange.getResponse();
            for(String excludeUrl:excludeUrlList){
                if(pathMatcher.match(excludeUrl,url)){
                    return chain.filter(mutableExchange);
                }
            }
            String token;
            String refreshToken;
            boolean isRequestParamVerifyToken = requestParamVerifyTokenUrls.stream().anyMatch(requestParamUrl->pathMatcher.match(requestParamUrl,url));
            if(isRequestParamVerifyToken){
                token = exchange.getRequest().getQueryParams().getFirst(X_ACCESS_TOKEN);
                refreshToken = exchange.getRequest().getQueryParams().getFirst(REFRESH_X_ACCESS_TOKEN);
            }else{
                token = exchange.getRequest().getHeaders().getFirst(X_ACCESS_TOKEN);
                refreshToken = exchange.getRequest().getHeaders().getFirst(REFRESH_X_ACCESS_TOKEN);
            }
            if (StringUtils.isEmpty(token)) {
                return getVoidMono(resp, 500,"token为空");
            }
            if (StringUtils.isEmpty(refreshToken)) {
                return getVoidMono(resp, 500,"refreshToken为空");
            }
            /*Result<VerifyToken> result = commonApi.verifyToken(token,refreshToken);
            if(!result.isSuccess()){
                return getVoidMono(resp, 500,result.getMessage());
            }
            if(result.getResult().isRefresh()){
                Token newToken =result.getResult().getToken();
                resp.getHeaders().set("token",newToken.getToken());
                resp.getHeaders().set("refreshToken",newToken.getRefreshToken());
                resp.getHeaders().set("Access-Control-Expose-Headers","token,refreshToken");
            }*/
            Mono<Void> resultMono = chain.filter(mutableExchange);
            return resultMono;
        }catch (Exception e){
            log.error("gate way filter error",e);
            throw e;
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }


    private Mono<Void> getVoidMono(ServerHttpResponse serverHttpResponse, int errorCode,String errorMsg) {
        serverHttpResponse.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        Result result = Result.error(errorCode, errorMsg);
        DataBuffer dataBuffer = serverHttpResponse.bufferFactory().wrap(JSONObject.toJSONString(result).getBytes());
        return serverHttpResponse.writeWith(Flux.just(dataBuffer));
    }
}
