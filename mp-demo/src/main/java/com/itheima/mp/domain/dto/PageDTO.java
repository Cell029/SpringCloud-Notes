package com.itheima.mp.domain.dto;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页结果")
public class PageDTO<V> {
    @Schema(description ="总条数")
    private Long total;
    @Schema(description ="总页数")
    private Long pages;
    @Schema(description ="返回的数据集合")
    private List<V> list;

    /**
     * 返回空分页结果
     * @param p MybatisPlus的分页结果
     * @param <V> 目标VO类型
     * @param <P> 原始PO类型
     * @return VO的分页对象
     */
    public static <V, P> PageDTO<V> empty(Page<P> p){
        return new PageDTO<>(p.getTotal(), p.getPages(), Collections.emptyList());
    }

    /**
     * 将MybatisPlus分页结果转为 VO分页结果
     * @param p MybatisPlus的分页结果
     * @param voClass 目标VO类型的字节码
     * @param <VO> 目标VO类型
     * @param <PO> 原始PO类型
     * @return VO的分页对象
     */
    public static <VO, PO> PageDTO<VO> of(Page<PO> p, Class<VO> voClass) {
        // 1. 非空校验
        List<PO> records = p.getRecords();
        if (records == null || records.isEmpty()) {
            // 无数据，返回空结果
            return empty(p);
        }
        // 2. 数据转换
        List<VO> vos = BeanUtil.copyToList(records, voClass);
        // 3. 封装返回
        return new PageDTO<>(p.getTotal(), p.getPages(), vos);
    }

    /**
     * 将MybatisPlus分页结果转为 VO分页结果，允许用户自定义PO到VO的转换方式
     * @param p MybatisPlus的分页结果
     * @param convertor PO到VO的转换函数
     * @param <VO> 目标VO类型
     * @param <PO> 原始PO类型
     * @return VO的分页对象
     */
    public static <VO, PO> PageDTO<VO> of(Page<PO> p, Function<PO, VO> convertor) {
        // 1. 非空校验
        List<PO> records = p.getRecords();
        if (records == null || records.isEmpty()) {
            // 无数据，返回空结果
            return empty(p);
        }
        // 2. 数据转换
        List<VO> vos = records.stream().map(convertor).collect(Collectors.toList());
        // 3. 封装返回
        return new PageDTO<>(p.getTotal(), p.getPages(), vos);
    }
}
