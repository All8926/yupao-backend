package com.example.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamQuitRequest implements Serializable {

    private static final long serialVersionUID = -3517483595740020936L;
    /**
     * 房间id
     */
    private Long teamId;
}
