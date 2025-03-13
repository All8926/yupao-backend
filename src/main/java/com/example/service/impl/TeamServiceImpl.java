package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.ErrorCode;
import com.example.dto.TeamQuery;
import com.example.exception.BusinessException;
import com.example.mapper.TeamMapper;
import com.example.model.domain.Team;
import com.example.model.domain.User;
import com.example.model.domain.UserTeam;
import com.example.model.enums.TeamStatusEnum;
import com.example.model.request.TeamJoinRequest;
import com.example.model.request.TeamQuitRequest;
import com.example.model.request.TeamUpdateRequest;
import com.example.model.vo.TeamUserVO;
import com.example.model.vo.UserVO;
import com.example.service.TeamService;

import com.example.service.UserService;
import com.example.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author Administrator
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2025-03-07 21:23:30
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private TeamMapper teamMapper;
    
    @Resource
    private UserService userService;

    /**
     * 创建队伍
     *
     * @param team
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {


        // 1.team 参数不能为空
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.用户必须登录
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        final long userId = loginUser.getId();
        // 3.校验信息
        //   1.队伍人数 > 1 且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数 > 1 且 <= 20");
        }
        //   2.队伍标题 <= 20
        String teamName = team.getName();
        if (StringUtils.isBlank(teamName) || teamName.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍名称长度不满足要求");
        }
        //   3.描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
        //   4.是否公开 不传默认公开(0)
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不存在");
        }
        //   5.如果是加密队伍必须设置密码且 密码必须不为空且 <= 32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRT.equals(statusEnum) && (StringUtils.isBlank(password) || password.length() > 32)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过长");
        }
        //   6.超时时间必须 > 当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间必须 > 当前时间");
        }
        //   7.一个用户最多创建5个队伍
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("userId", userId);
        long teamCount = this.count(teamQueryWrapper);
        if (teamCount >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "一个用户最多创建5个队伍");
        }
        //   8.插入队伍信息到队伍表
        team.setUserId(userId);
        boolean result = this.save(team);
        long teamId = team.getId();
        if (!result || teamId <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }
        //   9.插用户到用户队伍关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(userId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }

        return teamId;
    }

    /**
     * 根据条件查询队伍列表（含队友列表）
     * @param teamQuery
     * @return
     */
    @Override
    public List<TeamUserVO> listTeamAndUserList(TeamQuery teamQuery,boolean isAdmin) {
        Integer status = teamQuery.getStatus();
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if(statusEnum == null && !isAdmin){
            statusEnum = TeamStatusEnum.PUBLIC;
            teamQuery.setStatus(statusEnum.getValue());
        }
        // 非管理员不能查询私有状态的队伍
        if(!isAdmin && !statusEnum.equals(TeamStatusEnum.PUBLIC)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        List<TeamUserVO> teamListJoinUser = teamMapper.getTeamList(teamQuery);

        return teamListJoinUser;
    }

    /**
     * 根据条件查询队伍列表（含队长信息）
     * @param teamQuery
     * @return
     */
    @Override
    public List<TeamUserVO> listTeamAndCreateUser(TeamQuery teamQuery,boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 添加查询条件
        if(teamQuery != null){
            String name = teamQuery.getName();
            List<Long> idList = teamQuery.getIdList();
            if(!CollectionUtils.isEmpty(idList)){
                queryWrapper.in("id",idList);
            }
            //根据名称查询
            if(StringUtils.isNotBlank(name)){
                queryWrapper.like("name",name);
            }
            String searchText = teamQuery.getSearchText();
            //根据创建人id查询
            Long userId = teamQuery.getUserId();
            if(userId != null && userId > 0){
                queryWrapper.eq("userId",userId);
            }
            //根据队伍状态查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if(statusEnum == null && !isAdmin){
                statusEnum = TeamStatusEnum.PUBLIC;
                queryWrapper.eq("status",statusEnum.getValue());
            }
            //   非管理员不能查询私密状态的队伍
            if(!isAdmin && !statusEnum.equals(TeamStatusEnum.PUBLIC)){
               throw new BusinessException(ErrorCode.NO_AUTH);
            }


            //根据最大人数查询
            Integer maxNum = teamQuery.getMaxNum();
            if(maxNum != null && maxNum >= 1){
                queryWrapper.eq("maxNum",maxNum);
            }
            //根据描述查询
            String description = teamQuery.getDescription();
            if(StringUtils.isNotBlank(description)){
                queryWrapper.like("description",description);
            }
            //根据关键字查询
            if(StringUtils.isNotBlank(searchText)){
                queryWrapper.and(qw ->  qw.like("name",name).or().like("description",description));
            }

        }
        // 不展示已过期的队伍
        queryWrapper.and(qw ->  qw.gt("expireTime",new Date()).or().isNull("expireTime"));
        List<Team> teamList = this.list(queryWrapper);
        if(CollectionUtils.isEmpty(teamList)){
            return new ArrayList<>();
        }
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        // 关联查询创建人信息
        for ( Team team : teamList){
            Long userId = team.getUserId();
            if(userId == null){
                continue;
            }
            User user = userService.getById(userId);
            user = userService.getSafetyUser(user);

            UserVO userVO = new UserVO();
            TeamUserVO teamUserVO = new TeamUserVO();

            if(user != null){
                BeanUtils.copyProperties(user,userVO);
            }
            BeanUtils.copyProperties(team,teamUserVO);

            teamUserVO.setCreateUser(userVO);
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    /**
     * 修改队伍信息
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if(teamUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamUpdateRequest.getId();
        // 查询队伍是否存在
        Team team = getTeamById(teamId);
        // 加密队伍必须设置密码
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if(statusEnum.equals(TeamStatusEnum.SECRT)){
            String password = teamUpdateRequest.getPassword();
            if(StringUtils.isBlank(password) || password.length() > 32 ){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码格式不正确");
            }
        }
        long teamUserId = team.getUserId();
        long loginUserId = loginUser.getId();
        // 只有管理员或者队伍的创建者才可以修改队伍信息
        if(teamUserId != loginUserId && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest,updateTeam);

        boolean result = this.updateById(updateTeam);
        return result;
    }

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
       if(teamJoinRequest == null){
           throw new BusinessException(ErrorCode.PARAMS_ERROR);
       }
       // 校验队伍必须存在
        Long teamId = teamJoinRequest.getTeamId();
        Team team = getTeamById(teamId);
        // 队伍必须未过期
        Date expireTime = team.getExpireTime();
        if(expireTime != null && expireTime.before(new Date())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已过期");
        }
        // 不能加入私有队伍，私密房间需要校验密码
        Integer teamStatus = team.getStatus();
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamStatus);
        if(TeamStatusEnum.PRIVATE.equals(statusEnum)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"禁止加入私有房间");
        }
        String password = team.getPassword();
        if(TeamStatusEnum.SECRT.equals(statusEnum) ){
            if(StringUtils.isBlank(password) || !password.equals(teamJoinRequest.getPassword())){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"房间密码错误");
            }

        }
        Long userId = loginUser.getId();
        // 查询已加入的队伍数量
        long hasJoinTeamCount = this.userTeamCount(userId, null);
        if(hasJoinTeamCount >= 5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"最多只能创建和加入5个队伍");
        }

        // 查询是否重复加入
        long hasTeamUserCount = this.userTeamCount(userId, teamId);
        if(hasTeamUserCount > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"不能重复加入");
        }
        // 查询队伍是否已满
        long hasTeamCount = this.userTeamCount(null, teamId);
        if(hasTeamCount >= team.getMaxNum()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍人数已满");
        }

        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        // 加入队伍，往用户队伍关系表插入数据
       return userTeamService.save(userTeam);
    }

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     */
    @Override
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if(teamQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 检查队伍是否存在
        Long teamId = teamQuitRequest.getTeamId();
        Team team = getTeamById(teamId);
        // 检查我是否已加入该队伍
        long userId = loginUser.getId();
        long hasTeamUserCount = this.userTeamCount(userId, teamId);
        if(hasTeamUserCount < 1){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"未加入该队伍");
        }

        long teamCount = this.userTeamCount(null, teamId);
        //队伍只剩一人，删除队伍
        if(teamCount <= 1){
            this.removeById(teamId);
        }else {
            // 队伍还有其他人（>=2人）,检查自己是否为队长
            // 是的话转移队长身份到最早加入的成员
            long teamUserId = team.getUserId();
            if(userId == teamUserId){
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId",teamId);
                userTeamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if(CollectionUtils.isEmpty(userTeamList) || userTeamList.size() < 2){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"队伍人数不足2");
                }
                UserTeam nextUserTeam = userTeamList.get(1);
                Long nextUserLeaderId = nextUserTeam.getUserId();

                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextUserLeaderId);
                boolean result = this.updateById(updateTeam);
                if(!result){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新队长失败");
                }
            }
        }
        QueryWrapper<UserTeam>  queryWrapper = new QueryWrapper<>();
         queryWrapper.eq("teamId",teamId);
         queryWrapper.eq("userId",userId);
        return userTeamService.remove(queryWrapper);

    }


    /**
     * 删除（解散）队伍
     * @param teamId
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long teamId, User loginUser) {
        // 检查队伍是否存在
        Team team = getTeamById(teamId);
        long teamUserId = team.getUserId();
        long userId = loginUser.getId();
        // 检查是否为队长
        if(teamUserId != userId){
            throw new BusinessException(ErrorCode.NO_AUTH,"非队长不能解散队伍");
        }
        // 移除所加入队伍的关联信息
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId",teamId);
        boolean result = userTeamService.remove(queryWrapper);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除队伍的关联信息失败");
        }
        // 删除队伍
      return this.removeById(teamId);
    }

    /**
     * 根据id查询队伍
     * @param teamId
     * @return
     */
    private Team getTeamById(Long teamId) {
        if(teamId ==null || teamId < 1){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if(team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        return team;
    }

    /**
     * 根据用户id和队伍id查询队伍人数
     * @param userId
     * @param teamId
     * @return
     */
    private long userTeamCount(Long userId, Long teamId){
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        if(userId != null){
            queryWrapper.eq("userId",userId);
        }
        if(teamId != null){
            queryWrapper.eq("teamId",teamId);
        }

        long count = userTeamService.count(queryWrapper);
        return count;
    }
}




