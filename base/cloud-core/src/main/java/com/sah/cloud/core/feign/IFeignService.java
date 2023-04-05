package com.sah.cloud.core.feign;

public interface IFeignService {

    <T> T newInstance(Class<T> apiType, String name);
}
