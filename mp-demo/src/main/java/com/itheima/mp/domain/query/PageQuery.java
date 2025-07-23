package com.itheima.mp.domain.query;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(description = "分页查询实体")
public class PageQuery {
    @Schema(description = "页码")
    private Integer pageNo = 1;
    @Schema(description = "每页数据条数")
    private Integer pageSize = 5;
    @Schema(description = "排序字段")
    private String sortBy;
    @Schema(description = "是否升序")
    private Boolean isAsc = true;

    public <T> Page<T> toMpPage(OrderItem... orders) {
        // 1. 分页条件
        Page<T> page = Page.of(pageNo, pageSize);
        // 2. 排序条件，先看前端有没有传排序字段
        if (sortBy != null && !sortBy.isEmpty()) {
            page.addOrder(OrderItem.asc(sortBy));
            return page;
        }
        // 如果前端没有设置排序字段，则根据传递来的排序字段进行排序，
        // 例如：toMpPage(OrderItem.asc("username"), OrderItem.desc("create_time"))
        if (orders != null && orders.length > 0) {
            page.addOrder(orders);
        }
        return page;
    }

    public <T> Page<T> toMpPage(String defaultSortBy, boolean isAsc) {
        if (defaultSortBy != null && !defaultSortBy.isEmpty()) {
            if (!isAsc) {
                return this.toMpPage(OrderItem.desc(defaultSortBy));
            }
        }
        return this.toMpPage(OrderItem.asc(defaultSortBy));
    }

    public <T> Page<T> toMpPageDefaultSortByCreateTimeDesc() {
        return toMpPage("create_time", false);
    }

    public <T> Page<T> toMpPageDefaultSortByUpdateTimeDesc() {
        return toMpPage("update_time", false);
    }
}
