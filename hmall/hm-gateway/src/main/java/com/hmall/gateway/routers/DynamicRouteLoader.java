package com.hmall.gateway.routers;

import cn.hutool.json.JSONUtil;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.hmall.common.utils.CollUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import com.alibaba.cloud.nacos.NacosConfigManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteLoader {

    private final RouteDefinitionWriter writer;
    private final NacosConfigManager nacosConfigManager;

    // 路由配置文件的id和分组
    private final String dataId = "gateway-routes.json";
    private final String group = "DEFAULT_GROUP";
    // 保存更新过的路由 id
    private final Set<String> routeIds = new HashSet<>();

    // Bean 初始化后再执行
    @PostConstruct
    public void initRouteConfigListener() throws NacosException {
        // 1. 注册监听器并首次拉取配置
        String configInfo = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, group, 5000, new Listener() {
                    @Override
                    public Executor getExecutor() { // 定义线程池
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        // 监听到配置变更，需要更新路由表
                        updateConfigInfo(configInfo);
                    }
                });
        // 2. 首次启动时，更新一次配置
        updateConfigInfo(configInfo);
    }

    private void updateConfigInfo(String configInfo) {
        log.debug("监听到路由配置变更：{}", configInfo);
        // 1. 反序列化，将 json 文件转换成 RouteDefinition
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        // 2. 更新前先清空旧路由
        for (String routeId : routeIds) {
            writer.delete(Mono.just(routeId)).subscribe();
        }
        // 清空集合
        routeIds.clear();
        // 判断是否有新的路由要更新
        if (CollUtils.isEmpty(routeDefinitions)) {
            // 无新路由配置，直接结束
            return;
        }
        // 3. 更新路由
        routeDefinitions.forEach(routeDefinition -> {
            writer.save(Mono.just(routeDefinition)).subscribe();
            // 记录路由 id，方便将来删除
            routeIds.add(routeDefinition.getId());
        });
    }
}
