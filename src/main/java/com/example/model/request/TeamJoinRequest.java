package com.example.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TeamJoinRequest implements Serializable {

    private static final long serialVersionUID = -1438646530484367219L;
    /**
     * 房间id
     */
    private Long teamId;

    /**
     * 密码
     */
    private String password;
}
