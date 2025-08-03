package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.hmall.item.domain.dto.ItemDoc;
import com.hmall.item.domain.po.Item;
import com.hmall.item.jacksonJsonpMapper.CustomJacksonJsonpMapper;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(properties = "spring.profiles.active=local")
public class DocumentTest {

    private static RestClient restClient;
    private static ElasticsearchClient esClient;
    @Autowired
    private IItemService itemService;

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
        ElasticsearchTransport transport = new RestClientTransport(restClient, new CustomJacksonJsonpMapper());
        // 初始化 ElasticsearchClient
        esClient = new ElasticsearchClient(transport);
    }

    @AfterAll
    public static void tearDown() throws IOException {
        restClient.close();
    }

    @Test
    void testAddDocument() throws IOException {
        // 根据 id 查询商品数据
        Item item = itemService.getById(2627839L);
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        IndexResponse response = esClient.index(i -> i
                .index("items")
                .id(itemDoc.getId()) // 指定文档 ID
                .document(itemDoc)  // 要保存的对象
        );
        System.out.println("索引结果：" + response.result());
    }

    @Test
    void testGetDocumentById() throws IOException {
        String indexName = "items";
        String docId = "2627839";
        // 执行 GET 请求
        GetResponse<ItemDoc> response = esClient.get(g -> g
                        .index(indexName)
                        .id(docId),
                ItemDoc.class // 返回的实体类型，自动反序列化
        );
        if (response.found()) {
            ItemDoc itemDoc = response.source();
            System.out.println("查询到文档：" + itemDoc);
        } else {
            System.out.println("未找到文档，ID: " + docId);
        }
    }

    @Test
    void testDeleteDocument() throws IOException {
        String indexName = "items";
        String docId = "2627839"; // 要删除的文档ID
        DeleteResponse response = esClient.delete(d -> d
                .index(indexName)
                .id(docId)
        );
        if (response.result().name().equals("Deleted")) {
            System.out.println("文档删除成功，ID：" + docId);
        } else if (response.result().name().equals("NotFound")) {
            System.out.println("文档不存在，无法删除，ID：" + docId);
        } else {
            System.out.println("删除操作结果：" + response.result().name());
        }
    }

    @Test
    void testFullUpdateDocument() throws IOException {
        // 根据 id 查询商品数据
        Item item = itemService.getById(2627839L);
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        itemDoc.setName("test");
        IndexResponse response = esClient.index(i -> i
                .index("items")
                .id("1")
                .document(itemDoc) // 传入完整文档对象
        );
        System.out.println("返回结果: " + response.result());
    }

    @Test
    void testPartialUpdateDocument() throws IOException {
        Map<String, Object> updateFields = new HashMap<>();
        updateFields.put("price", 2999);
        updateFields.put("sold", 100);
        UpdateResponse<ItemDoc> response = esClient.update(u -> u
                        .index("items")
                        .id("1")
                        .doc(updateFields),
                ItemDoc.class
        );
        System.out.println("返回结果: " + response.result());
    }

    @Test
    void testBulk() throws IOException {
        List<ItemDoc> items = new ArrayList<>();
        List<BulkOperation> operations = new ArrayList<>();

        // 根据 id 查询商品数据
        Item item1 = itemService.getById(2627839L);
        ItemDoc itemDoc1 = BeanUtil.copyProperties(item1, ItemDoc.class);
        Item item2 = itemService.getById(2627072L);
        ItemDoc itemDoc2 = BeanUtil.copyProperties(item2, ItemDoc.class);
        items.add(itemDoc1);
        items.add(itemDoc2);

        for (ItemDoc item : items) {
            BulkOperation op = new BulkOperation.Builder()
                    .index(idx -> idx
                            .index("items")
                            .id(item.getId())
                            .document(item)
                    )
                    .build();
            operations.add(op);
        }
        BulkRequest bulkRequest = new BulkRequest.Builder()
                .operations(operations)
                .build();
        BulkResponse response = esClient.bulk(bulkRequest);
        if (response.errors()) {
            System.out.println("部分文档导入失败，失败项如下：");
            response.items().forEach(item -> {
                if (item.error() != null) {
                    System.out.println(item.error().reason());
                }
            });
        } else {
            System.out.println("所有文档导入成功！");
        }
    }

}
