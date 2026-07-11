package com.ar.laboratory.asyncjobengine.shared.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuración de caché con Redis
 *
 * <p>Características: - TTL configurable por cacheName - Serialización JSON con Jackson -
 * KeyGenerator determinístico - Logging de cache hits/misses - Habilitado por configuración:
 * app.cache.enabled
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = false)
@Profile("!test")
public class CacheConfig implements CachingConfigurer {

    // Nombres de caché con TTL de 30 segundos
    public static final String CACHE_30_SECONDS = "cache30s";
    public static final String EXAMPLES_CACHE = "examplesCache";

    // Nombres de caché legacy (mantener compatibilidad)
    public static final String EXAMPLES_BY_DNI = "examplesByDni";

    /**
     * Configuración del CacheManager con Redis y TTL específicos por cacheName
     *
     * @param connectionFactory factory de conexión Redis
     * @return CacheManager configurado
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configurando RedisCacheManager con TTL específicos por cacheName");

        // Configuración por defecto (10 minutos)
        RedisCacheConfiguration defaultConfig = createCacheConfiguration(Duration.ofMinutes(10));

        // Configuraciones específicas por cacheName
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Cachés con TTL de 30 segundos
        RedisCacheConfiguration config30s = createCacheConfiguration(Duration.ofSeconds(30));
        cacheConfigurations.put(CACHE_30_SECONDS, config30s);
        cacheConfigurations.put(EXAMPLES_CACHE, config30s);
        cacheConfigurations.put(EXAMPLES_BY_DNI, config30s);

        log.info("Cachés configurados con TTL=30s: {}", cacheConfigurations.keySet());

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * Crea una configuración de caché con el TTL especificado
     *
     * @param ttl tiempo de vida del caché
     * @return configuración de caché
     */
    private RedisCacheConfiguration createCacheConfiguration(Duration ttl) {
        // Configurar ObjectMapper para serialización JSON con type info
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(objectMapper)))
                .disableCachingNullValues();
    }

    /**
     * KeyGenerator determinístico que genera claves de caché consistentes
     *
     * <p>La clave generada incluye: - Nombre de la clase - Nombre del método - Parámetros en orden
     * determinístico
     *
     * @return KeyGenerator personalizado
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new DeterministicKeyGenerator();
    }

    /**
     * Error handler para fallos de caché (no fallar la operación si Redis falla)
     *
     * @return CacheErrorHandler personalizado
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheConfig.LoggingCacheErrorHandler();
    }

    /** KeyGenerator determinístico para generar claves consistentes */
    public static class DeterministicKeyGenerator implements KeyGenerator {

        @Override
        public Object generate(Object target, Method method, Object... params) {
            StringBuilder keyBuilder = new StringBuilder();

            // Agregar clase y método
            keyBuilder.append(target.getClass().getSimpleName());
            keyBuilder.append(".");
            keyBuilder.append(method.getName());

            // Agregar parámetros en orden determinístico
            if (params != null && params.length > 0) {
                keyBuilder.append(":");
                keyBuilder.append(Arrays.toString(params));
            }

            String key = keyBuilder.toString();
            log.trace("Generated cache key: {}", key);

            return key;
        }
    }

    /** Error handler para logging de errores de caché */
    private static class LoggingCacheErrorHandler implements CacheErrorHandler {

        @Override
        public void handleCacheGetError(
                RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
            log.error(
                    "Cache GET error - cache: {}, key: {}, error: {}",
                    cache.getName(),
                    key,
                    exception.getMessage());
        }

        @Override
        public void handleCachePutError(
                RuntimeException exception,
                org.springframework.cache.Cache cache,
                Object key,
                Object value) {
            log.error(
                    "Cache PUT error - cache: {}, key: {}, error: {}",
                    cache.getName(),
                    key,
                    exception.getMessage());
        }

        @Override
        public void handleCacheEvictError(
                RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
            log.error(
                    "Cache EVICT error - cache: {}, key: {}, error: {}",
                    cache.getName(),
                    key,
                    exception.getMessage());
        }

        @Override
        public void handleCacheClearError(
                RuntimeException exception, org.springframework.cache.Cache cache) {
            log.error(
                    "Cache CLEAR error - cache: {}, error: {}",
                    cache.getName(),
                    exception.getMessage());
        }
    }
}
