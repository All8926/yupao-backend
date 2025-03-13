package com.example.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用删除请求参数
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = -1936016492607835043L;
    /**
     * 删除id
     */
    private long id;
}
