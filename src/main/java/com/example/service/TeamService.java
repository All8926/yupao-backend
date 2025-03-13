package com.example.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dto.TeamQuery;
import com.example.model.domain.Team;
import com.example.model.domain.User;
import com.example.model.request.TeamJoinRequest;
import com.example.model.request.TeamQuitRequest;
import com.example.model.request.TeamUpdateRequest;
import com.example.model.vo.TeamUserVO;

import java.util.List;

/**
* @author Administrator
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2025-03-07 21:23:30
*/
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

    /**
     * 根据条件查询队伍列表（含队友列表）
     * @param teamQuery
     * @return
     */
    List<TeamUserVO> listTeamAndUserList(TeamQuery teamQuery,boolean isAdmin);

    /**
     * 根据条件查询队伍列表（含队长信息）
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    List<TeamUserVO> listTeamAndCreateUser(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 修改队伍信息
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 删除（解散）队伍
     * @param teamId
     * @param loginUser
     * @return
     */
    boolean deleteTeam(long teamId, User loginUser);
}
