package com.hmall.item.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ElasticsearchClientTest {

    private static RestClient restClient;
    private static ElasticsearchClient esClient;

    @BeforeAll
    public static void setup() {

        // 设置认证信息
        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", "ixLEd4xRavGVcE=PLVrA"));
        // 创建带认证信息的 RestClient
        restClient = RestClient.builder(
                        new HttpHost("localhost", 9200, "http"))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                )
                .build();
        // 使用 Jackson JSON 映射器创建 Transport
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        // 初始化 ElasticsearchClient
        esClient = new ElasticsearchClient(transport);
    }

    @AfterAll
    public static void tearDown() throws IOException {
        restClient.close();
    }

    @Test
    public void testConnection() throws IOException {
        InfoResponse info = esClient.info();
        assertNotNull(info);
        System.out.println("esClient: " + esClient);
        System.out.println("restClient: " + restClient);
        System.out.println("info: " + info);
        System.out.println("连接成功，版本号: " + info.version().number());
    }

    @Test
    public void testCreateItemsIndex() throws IOException {
        String indexName = "items";
        ElasticsearchIndicesClient indices = esClient.indices();
        CreateIndexResponse response = indices.create(c -> c
                .index(indexName)
                .mappings(m -> m
                        .properties("id", p -> p.keyword(k -> k))
                        .properties("name", p -> p
                                .text(t -> t.analyzer("ik_max_word"))
                        )
                        .properties("price", p -> p.integer(i -> i))
                        .properties("stock", p -> p.integer(i -> i))
                        .properties("image", p -> p
                                .keyword(k -> k.index(false))
                        )
                        .properties("category", p -> p.keyword(k -> k))
                        .properties("brand", p -> p.keyword(k -> k))
                        .properties("sold", p -> p.integer(i -> i))
                        .properties("commentCount", p -> p
                                .integer(i -> i.index(false))
                        )
                        .properties("isAD", p -> p.boolean_(b -> b))
                        .properties("updateTime", p -> p.date(d -> d))
                )
        );
        // junit 5 的断言方法，内部为 true 时则正常，为 false 时报错
        assertTrue(response.acknowledged());
        System.out.println("索引创建成功：" + indexName);
    }

    @Test
    void testDeleteIndex() throws IOException {
        // 1. 获取索引客户端
        ElasticsearchIndicesClient indicesClient = esClient.indices();
        // 2. 指定要删除的索引名称并发送删除请求
        DeleteIndexResponse response = indicesClient.delete(d -> d.index("items"));
        assertTrue(response.acknowledged());
        System.out.println("索引删除成功！");
    }

    @Test
    void testExistsIndex() throws IOException {
        BooleanResponse response = esClient.indices().exists(e -> e.index("items"));
        if (response.value()) {
            System.out.println("索引存在");
        } else {
            System.out.println("索引不存在");
        }
    }
}
