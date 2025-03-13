package com.example.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页请求参数
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = -3964202769308949174L;
    /**
     * 一页的数量
     */
    protected int pageSize = 10;

    /**
     * 页码
     */
    protected int pageNum = 1;
}
