package com.example.service;

import com.example.dto.TeamQuery;
import com.example.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.model.vo.TeamUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.example.constant.UserConstant.ADMIN_ROLE;
import static com.example.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author Administrator
* @description 针对表【user】的数据库操作Service
* @createDate 2024-10-09 21:15:42
*/
public interface UserService extends IService<User> {

    /**
     *  用户注册
     * @param userAccount   账号
     * @param userPassword  密码
     * @param checkPassword 确认密码
     * @param planetCode 编号
     * @return 用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     *
     * @param userAccount  账号
     * @param userPassword 密码
     * @param request
     * @return 脱敏后的用户信息
     */
    User doLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     * @param originUser
     * @return
     */
    User getSafetyUser(User originUser);

    /**
     * 用户注销
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签列表查询用户
     * @param tagNameList
     * @return
     */
    List<User> searchUserByTags(List<String> tagNameList);

    /**
     *
     * @param user 要修改用户
     * @param loginUser 当前登录用户
     * @return
     */
    int updateUser(User user, User loginUser);

    /**
     * 是否为管理员
     *
     * @param request
     * @return true-是 false-否
     */
     boolean isAdmin(HttpServletRequest request);
     boolean isAdmin(User user);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
     User getLoginUser(HttpServletRequest request);


    /**
     * 匹配用户
     * @param num
     * @param loginUser
     * @return
     */
    List<User> matchUsers(Long num, User loginUser);
}
