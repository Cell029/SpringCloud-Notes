package com.hmall.item.jacksonJsonpMapper;

import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class CustomJacksonJsonpMapper extends JacksonJsonpMapper {
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        // 注册 Java8 时间模块
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }
    public CustomJacksonJsonpMapper() {
        super(OBJECT_MAPPER);
    }
}
