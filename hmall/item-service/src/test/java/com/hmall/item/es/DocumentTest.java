package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
        Item item = itemService.getById(1861099L);
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
        String docId = "1861100"; // 要删除的文档ID
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
        Item item1 = itemService.getById(1861100L);
        ItemDoc itemDoc1 = BeanUtil.copyProperties(item1, ItemDoc.class);
        Item item2 = itemService.getById(1861101L);
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

    @Test
    void testMatchAll() throws IOException {
        SearchResponse<ItemDoc> response = esClient.search(s -> s
                        .index("items")
                        .query(q -> q // 定义查询条件
                                .match(m -> m // 使用 match 查询
                                        .field("name") // 要查询的字段
                                        .query("男女") // 查询关键词
                                )
                        ),
                ItemDoc.class // 结果映射的实体类
        );
        for (Hit<ItemDoc> hit : response.hits().hits()) {
            System.out.println(hit.source());
        }
    }

    @Test
    void testMatchAll2() throws IOException {
        SearchResponse<ItemDoc> response = esClient.search(s -> s
                        .index("items")
                        .query(q -> q
                                .matchAll(m -> m) // 重点：matchAll 查询
                        ),
                ItemDoc.class
        );
        for (Hit<ItemDoc> hit : response.hits().hits()) {
            System.out.println(hit.source());
        }
    }

    @Test
    void testMatch() throws IOException {
        SearchResponse<ItemDoc> response = esClient.search(s -> s
                        .index("items")
                        .query(q -> q
                                .match(m -> m
                                        .field("name")
                                        .query("男女")
                                )
                        ),
                ItemDoc.class
        );
        for (Hit<ItemDoc> hit : response.hits().hits()) {
            System.out.println(hit.source());
        }
    }

    @Test
    void testRange() throws IOException {
        SearchResponse<ItemDoc> response = esClient.search(s -> s
                        .index("items")
                        .query(q -> q
                                .range(r -> r
                                        .field("price")
                                        .gte(JsonData.of(1000))
                                        .lte(JsonData.of(30000))
                                )
                        ),
                ItemDoc.class
        );
        for (Hit<ItemDoc> hit : response.hits().hits()) {
            System.out.println(hit.source());
        }
    }

    @Test
    void testMultiMatch() throws IOException {
        SearchResponse<ItemDoc> response = esClient.search(s -> s
                        .index("items")
                        .query(q -> q
                                .multiMatch(m -> m
                                        .query("拉杆")
                                        .fields("name", "brand", "category")
                                )
                        ),
                ItemDoc.class
        );
        for (Hit<ItemDoc> hit : response.hits().hits()) {
            System.out.println(hit.source());
        }
    }

    @Test
    void testTermQuery() throws IOException {
        SearchResponse<ItemDoc> response = esClient.search(s -> s
                        .index("items")
                        .query(q -> q
                                .term(t -> t
                                        .field("brand")
                                        .value("博兿")
                                )
                        ),
                ItemDoc.class
        );
        for (Hit<ItemDoc> hit : response.hits().hits()) {
            System.out.println(hit.source());
        }
    }

    @Test
    void testBool() throws IOException {
        SearchResponse<ItemDoc> response = esClient.search(s -> s
                        .index("items")
                        .query(q -> q
                                .bool(b -> b
                                        .must(must -> must
                                                .match(mt -> mt.field("name").query("手机"))
                                        )
                                        .must(must -> must
                                                .match(mt -> mt.field("category").query("手机"))
                                        )
                                        .filter(f -> f
                                                .range(r -> r
                                                        .field("price")
                                                        .gte(JsonData.of(10000))
                                                        .lte(JsonData.of(70000))
                                                )
                                        )
                                        .should(sh -> sh
                                                .term(t -> t.field("brand").value("Apple"))
                                        )
                                        .should(sh -> sh
                                                .term(t -> t.field("brand").value("华为"))
                                        )
                                        .mustNot(mn -> mn
                                                .term(t -> t.field("brand").value("博兿"))
                                        )
                                        .minimumShouldMatch("1") // 至少满足一个 should 条件
                                )
                        ),
                ItemDoc.class
        );
        for (Hit<ItemDoc> hit : response.hits().hits()) {
            System.out.println(hit.source());
        }
    }

    @Test
    void testPageAndSort() throws IOException {
        int pageNum = 1;
        int pageSize = 10;
        int from = (pageNum - 1) * pageSize; // 起始文档偏移量

        SearchResponse<ItemDoc> response = esClient.search(s -> s
                        .index("items")
                        .from(from) // 分页起始位置
                        .size(pageSize) // 每页条数
                        .query(q -> q
                                .bool(b -> b
                                        .must(m -> m.match(mt -> mt.field("name").query("手机")))
                                        .filter(f -> f.range(r -> r.field("price").gte(JsonData.of(10000)).lte(JsonData.of(70000))))
                                )
                        )
                        .sort(List.of(
                                SortOptions.of(s1 -> s1.field(f -> f.field("price").order(SortOrder.Asc))), // 价格升序
                                SortOptions.of(s2 -> s2.field(f -> f.field("updateTime").order(SortOrder.Desc).missing("_last"))) // 更新时间降序，缺失排后面
                        )),
                ItemDoc.class
        );
        for (Hit<ItemDoc> hit : response.hits().hits()) {
            System.out.println(hit.source());
        }

    }

    @Test
    void testHighLight() throws IOException {
        SearchResponse<ItemDoc> response = esClient.search(s -> s
                        .index("items")
                        .query(q -> q
                                .match(m -> m
                                        .field("name")
                                        .query("手机")
                                )
                        )
                        .highlight(h -> h
                                .fields("name", f -> f
                                        .preTags("<em>")
                                        .postTags("</em>")
                                )
                        ),
                ItemDoc.class
        );
        for (Hit<ItemDoc> hit : response.hits().hits()) {
            ItemDoc source = hit.source();
            // 获取高亮字段的结果，是一个 Map<String, List<String>>，key 是字段名（如 "name"），value 是高亮后的若干个片段（可能是多个）
            Map<String, List<String>> highlight = hit.highlight();
            Assertions.assertNotNull(source);
            System.out.println("原始 name：" + source.getName());
            if (highlight != null && highlight.containsKey("name")) {
                List<String> fragments = highlight.get("name");
                System.out.println("高亮片段：" + String.join("...", fragments));
            }
        }
    }

    @Test
    void testAgg() throws IOException {
        SearchResponse<ItemDoc> response = esClient.search(b -> b
                        .index("items")
                        .size(0)
                        .query(q -> q.term(t -> t.field("category").value("手机")))
                        .aggregations("brand_agg", a -> a
                                .terms(h -> h.field("brand"))
                        ),
                ItemDoc.class
        );

        List<StringTermsBucket> buckets = response.aggregations()
                .get("brand_agg")
                .sterms()
                .buckets()
                .array();
        for (StringTermsBucket bucket : buckets) {
            // 获取桶内 key
            System.out.println("Brand: " + bucket.key().stringValue() + ", Count: " + bucket.docCount());
        }
    }

    @Test
    void testMetricAgg() throws IOException {
        SearchResponse<ItemDoc> response = esClient.search(b -> b
                        .index("items")
                        .size(0) // 不返回文档，只返回聚合结果
                        .query(q -> q.term(t -> t.field("category").value("手机")))
                        .aggregations("max_price", a -> a.max(m -> m.field("price")))
                        .aggregations("min_price", a -> a.min(m -> m.field("price")))
                        .aggregations("avg_price", a -> a.avg(m -> m.field("price"))),
                ItemDoc.class
        );
        // 取出聚合结果
        double maxPrice = response.aggregations()
                .get("max_price")
                .max()
                .value();
        double minPrice = response.aggregations()
                .get("min_price")
                .min()
                .value();
        double avgPrice = response.aggregations()
                .get("avg_price")
                .avg()
                .value();
        System.out.println("Max price: " + maxPrice);
        System.out.println("Min price: " + minPrice);
        System.out.println("Avg price: " + avgPrice);
    }


}
