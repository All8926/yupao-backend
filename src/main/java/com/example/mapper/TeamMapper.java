package com.example.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dto.TeamQuery;
import com.example.model.domain.Team;
import com.example.model.vo.TeamUserVO;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;

/**
* @author Administrator
* @description 针对表【team(队伍)】的数据库操作Mapper
* @createDate 2025-03-07 21:23:30
* @Entity generator.domain.Team
*/
public interface TeamMapper extends BaseMapper<Team> {

    /**
     * 查询队伍列表
     * @param teamQuery
     * @return
     */
    List<TeamUserVO> getTeamList(  TeamQuery teamQuery );
}




