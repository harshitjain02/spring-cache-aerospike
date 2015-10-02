/*
 * Copyright 2015 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package v.a.org.springframework.cache.aerospike.config.annotation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import v.a.org.springframework.store.StoreCompression;
import v.a.org.springframework.store.serialization.KryoReflectionSupportSerializer;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.async.AsyncClient;
import com.aerospike.client.async.AsyncClientPolicy;
import com.aerospike.client.async.IAsyncClient;
import com.aerospike.client.policy.ClientPolicy;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EnableAerospikeCacheManagerKryoReflectionIT {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    @Named("aerospikeCacheManager")
    private CacheManager cacheManager;

    @Test
    public void when_contextStarted_thenNoExceptions() {
        log.info("Spring context loaded. Aerospike Cache manager: {}", cacheManager);
    }

    @Test
    public void defaultCacheCreated() {
        assertThat(cacheManager.getCache("ITDEFAULT"), notNullValue());
    }

    @Test
    public void storeObject_noDefaultConstructor() {
        String name = "ITPRECONF";
        Cache c = cacheManager.getCache(name);
        assertThat(c, notNullValue());

        StoredNoDefaultConstructor s = new StoredNoDefaultConstructor("ID", "NAME");
        c.put("NDC", s);
        StoredNoDefaultConstructor result = c.get("NDC", StoredNoDefaultConstructor.class);
        assertThat(result, notNullValue());
        assertThat(result.getId(), is("ID"));
        assertThat(result.getName(), is("NAME"));
    }

    /**
     * Cached class without default constructor.
     */
    static class StoredNoDefaultConstructor {
        private String id;
        private String name;

        public StoredNoDefaultConstructor(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @EnableAerospikeCacheManager(
            serializerClass = KryoReflectionSupportSerializer.class,
            compression = StoreCompression.SNAPPY,
            defaultNamespace = "cache",
            defaultCacheName = "ITDEFAULT",
            defaultTimeToLiveInSeconds = 300,
            caches = {
                    @AerospikeCacheConfig(name = "cache:ITPRECONF", timeToLiveInSeconds = 100)
            })
    @Configuration
    @PropertySource(value = "classpath:/application.properties")
    static class Config {

        @Inject
        private Environment env;

        @Bean(destroyMethod = "close")
        public IAerospikeClient aerospikeClient() throws Exception {
            final ClientPolicy defaultClientPolicy = new ClientPolicy();
            final IAerospikeClient client = new AerospikeClient(defaultClientPolicy,
                    new Host(env.getProperty("aerospike.host"),
                            Integer.valueOf(env.getProperty("aerospike.port"))));
            return client;
        }

        @Bean(destroyMethod = "close")
        public IAsyncClient aerospikeAsyncClient() throws Exception {
            final AsyncClientPolicy defaultAsyncClientPolicy = new AsyncClientPolicy();
            final IAsyncClient client = new AsyncClient(defaultAsyncClientPolicy,
                    new Host(env.getProperty("aerospike.host"),
                            Integer.valueOf(env.getProperty("aerospike.port"))));
            return client;
        }
    }
}
