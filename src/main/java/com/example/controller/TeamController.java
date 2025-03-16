package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BaseResponse;
import com.example.common.DeleteRequest;
import com.example.common.ErrorCode;
import com.example.common.ResultUtils;
import com.example.dto.TeamQuery;
import com.example.exception.BusinessException;
import com.example.model.domain.Team;
import com.example.model.domain.User;
import com.example.model.domain.UserTeam;
import com.example.model.request.*;
import com.example.model.vo.TeamUserVO;
import com.example.service.TeamService;
import com.example.service.UserService;
import com.example.service.UserTeamService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://localhost:5173"}, allowCredentials = "true") // 解决跨域
public class TeamController {
    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    /**
     * 创建队伍
     *
     * @param teamAddRequest
     * @param request
     * @return
     */
    @ApiOperation("创建队伍")
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long result = teamService.addTeam(team, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 删除（解散）队伍
     *
     * @param deleteRequest
     * @return
     */
    @ApiOperation("删除（解散）队伍")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.deleteTeam(id, loginUser);
        return ResultUtils.success(result);
    }


    /**
     * 修改队伍
     *
     * @param teamUpdateRequest
     * @param request
     * @return
     */
    @ApiOperation("修改队伍")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);

        return ResultUtils.success(result);
    }


    /**
     * 根据id查询队伍
     *
     * @param teamId
     * @return
     */
    @ApiOperation("根据id查询队伍")
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long teamId) {
        if (teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }


    /**
     * 根据条件查询队伍列表（含队友列表）
     *
     * @param teamQuery
     * @return
     */
    @ApiOperation("队伍列表（含队友列表）")
    @GetMapping("/list_user_list")
    public BaseResponse<List<TeamUserVO>> listTeamAndUserList(TeamQuery teamQuery, HttpServletRequest request) {
        boolean isAdmin = userService.isAdmin(request);
        List<TeamUserVO> teamUserList = teamService.listTeamAndUserList(teamQuery, isAdmin);

        return ResultUtils.success(teamUserList);
    }

    /**
     * 根据条件查询队伍列表（含队长信息）
     *
     * @param teamQuery
     * @return
     */
    @ApiOperation("队伍列表（含队长信息）")
    @GetMapping("/list_create_user")
    public BaseResponse<List<TeamUserVO>> listTeamAndCreateUser(TeamQuery teamQuery, HttpServletRequest request) {
        boolean isAdmin = userService.isAdmin(request);
        List<TeamUserVO> teamUserList = teamService.listTeamAndCreateUser(teamQuery, isAdmin);
        // 队伍id列表
        List<Long> teamIdList = teamUserList.stream().map(TeamUserVO::getId).collect(Collectors.toList());

        // 1.查询用户是否已加入队伍
        try {
            QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
            User loginUser = userService.getLoginUser(request);
            long userId = loginUser.getId();
            queryWrapper.eq("userId", userId);
            queryWrapper.in("teamId", teamIdList);
            // 我所加入的队伍id
            Set<Long> joinTeamIdList = userTeamService.list(queryWrapper).stream().map(UserTeam::getTeamId).collect(Collectors.toSet());

            // 判断我是否已加入队伍
            teamUserList.forEach(teamUserVO -> {
                boolean hasJoinTeam = joinTeamIdList.contains(teamUserVO.getId());
                teamUserVO.setHasJoinTeam(hasJoinTeam);
            });
        } catch (Exception e) {}

        // 2.查询队伍已加入的人数
        QueryWrapper<UserTeam> queryUserTeamWrapper = new QueryWrapper<>();
        queryUserTeamWrapper.in("teamId",teamIdList);
        // teamId => UserTeamList
        Map<Long, List<UserTeam>> teamIdUserTeamListMap = userTeamService.list(queryUserTeamWrapper).stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamUserList.forEach(teamUserVO -> {
            teamUserVO.setHasJoinTeamNum(teamIdUserTeamListMap.getOrDefault(teamUserVO.getId(),new ArrayList<>()).size());
        });
        return ResultUtils.success(teamUserList);
    }

    /**
     * 分页查询队伍列表
     *
     * @param teamQuery
     * @return
     */
    @ApiOperation("分页查询队伍列表")
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamByPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(page, queryWrapper);

        return ResultUtils.success(resultPage);
    }

    /**
     * 加入队伍
     *
     * @param teamJoinRequest
     * @param request
     * @return
     */
    @ApiOperation("加入队伍")
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 退出队伍
     *
     * @param teamQuitRequest
     * @param request
     * @return
     */
    @ApiOperation("退出队伍")
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 我创建的队伍
     *
     * @return
     */
    @ApiOperation("我创建的队伍")
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listTeamAdd(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        TeamQuery teamQuery = new TeamQuery();
        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVO> teamUserList = teamService.listTeamAndUserList(teamQuery, true);

        return ResultUtils.success(teamUserList);
    }

    /**
     * 我加入的队伍
     *
     * @return
     */
    @ApiOperation("我加入的队伍")
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listTeamJoin(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("userId", loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);

        List<Long> teamIdList = userTeamList.stream().map(item -> item.getTeamId()).distinct().collect(Collectors.toList());

        TeamQuery teamQuery = new TeamQuery();
        teamQuery.setIdList(teamIdList);
        // List<TeamUserVO> teamUserVOS = teamService.listTeamAndCreateUser(teamQuery, true);
        List<TeamUserVO> teamUserVOS = teamService.listTeamAndUserList(teamQuery, true);

        return ResultUtils.success(teamUserVOS);
    }

}
