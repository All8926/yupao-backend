package com.example.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mapper.UserTeamMapper;
import com.example.model.domain.UserTeam;
import com.example.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【user_team(用户队伍表)】的数据库操作Service实现
* @createDate 2025-03-07 21:34:58
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService {

}




