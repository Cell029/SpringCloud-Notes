package com.hmall.item.jacksonJsonpMapper;

import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class CustomJacksonJsonpMapper extends JacksonJsonpMapper {
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        // 禁止把日期序列化成数组，而是序列化成字符串
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 注册 Java8 时间模块
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }
    public CustomJacksonJsonpMapper() {
        super(OBJECT_MAPPER);
    }
}
