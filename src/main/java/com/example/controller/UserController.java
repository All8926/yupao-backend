package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BaseResponse;
import com.example.common.ErrorCode;
import com.example.common.ResultUtils;
import com.example.exception.BusinessException;
import com.example.model.domain.User;
import com.example.model.request.UserLoginRequest;
import com.example.model.request.UserRegisterRequest;
import com.example.service.UserService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.constant.UserConstant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"http://localhost:5173"},allowCredentials = "true") // 解决跨域
public class UserController {
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return 用户id
     */
    @PostMapping("/register")
    @ApiOperation("用户注册")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            return null;
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();

        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            return null;
        }
        Long userId = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(userId);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 账号
     * @param request          密码
     * @return 用户信息
     */
    @ApiOperation("用户登录")
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }
        User user = userService.doLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 获取用户信息
     *
     * @param request
     * @return
     */
    @ApiOperation("获取用户信息")
    @GetMapping("/current")
    public BaseResponse<User> currentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long id = currentUser.getId();
        User user = userService.getById(id);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @ApiOperation("用户注销")
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        int i = userService.userLogout(request);
        return ResultUtils.success(i);
    }

    /**
     * 用户搜索
     *
     * @param username 用户名
     * @return
     */
    @ApiOperation("用户搜索")
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUserByUsername(String username, HttpServletRequest request) {
        // 鉴权 仅管理员可搜索
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }

        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    @ApiOperation("用户推荐")
    @GetMapping("/recommend")
    public BaseResponse<Page> recommendUser(Long pageNum,Long pageSize, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String redisKey = String.format("yupao:user:recommend:%s",loginUser.getId());
        ValueOperations valueOperations = redisTemplate.opsForValue();

        // 判断 redis 中是否有缓存，有的话直接取
        Page<User> userPage = (Page<User>) valueOperations.get(redisKey);
        if(userPage != null){
            return  ResultUtils.success(userPage);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        userPage = userService.page(new Page<>(pageNum,pageSize),queryWrapper);
        // 没有缓存则添加到 redis，设置过期时间为 30s
        try {
            valueOperations.set(redisKey,userPage,30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResultUtils.success(userPage);
    }


    /**
     * 根据标签搜索用户
     * @param tagNameList
     * @return
     */
    @ApiOperation("根据标签搜索用户")
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUserByTags(@RequestParam(required = false) List<String> tagNameList) {
        if (tagNameList == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUserByTags(tagNameList);
        return ResultUtils.success(userList);
    }

    /**
     * 根据id删除用户
     *
     * @param
     * @return
     */
    @ApiOperation("根据id删除用户")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody User user, HttpServletRequest request) {
        // 鉴权 仅管理员可删除
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        if (user.getId() < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id不能小于1");
        }
        boolean b = userService.removeById(user.getId());
        return ResultUtils.success(b);
    }

    /**
     * 修改用户信息
     * @param user
     * @param request
     * @return
     */
    @ApiOperation("修改用户信息")
    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request){
        if(user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        int i = userService.updateUser(user, loginUser);
        return ResultUtils.success(i);
    }

}
