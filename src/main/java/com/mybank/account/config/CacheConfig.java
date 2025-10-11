package com.mybank.account.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * simple cache config,can use redis in the product env
 */
@Configuration
@EnableCaching
public class CacheConfig {

}