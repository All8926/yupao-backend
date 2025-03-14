package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.ErrorCode;
import com.example.exception.BusinessException;
import com.example.model.domain.User;
import com.example.service.UserService;
import com.example.mapper.UserMapper;
import com.example.utils.AlgorithmUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.constant.UserConstant.ADMIN_ROLE;
import static com.example.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    // 密码加盐，混淆密码
    private static final String SALT = "user";


    /**
     * 用户注册
     *
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 校验字段是否为空
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 账号不能小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        }

        // 密码不能小于6位
        if (userPassword.length() < 6 || checkPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能小于6位");
        }

        // 编号不能大于5位
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号不能大于5位");
        }

        // 账号不能包含特殊字符
        String validPattern = "^[a-zA-Z0-9_.]+$";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (!matcher.matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }

        // 密码和校验密码是否相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }

        // 账号不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();    // 创建 QueryWrapper 对象，用于构建查询条件
        queryWrapper.eq("userAccount", userAccount);    // 添加查询条件：userAccount 字段等于 userAccount 变量的值
//        long count = this.count(queryWrapper);    // 执行查询，统计满足条件的记录数
        Long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        // 编号不能重复
        queryWrapper = new QueryWrapper<>();    // 创建 QueryWrapper 对象，用于构建查询条件
        queryWrapper.eq("planetCode", planetCode);    // 添加查询条件：planetCode 字段等于 planetCode 变量的值
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号已存在");
        }

        // 加密
        String newPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(newPassword);
        user.setPlanetCode(planetCode);

        this.save(user);
        if (user.getId() == null) {
            throw new BusinessException("注册失败", 40000, "");
        }
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount  账号
     * @param userPassword 密码
     * @param request
     * @return 返回脱敏后的用户信息
     */
    @Override
    public User doLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 校验这2个字段是否为空
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }

        // 账号不能小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        }

        // 密码不能小于6位
        if (userPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能小于6位");
        }

        // 账号不能包含特殊字符
        String validPattern = "^[a-zA-Z0-9_.]+$";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (!matcher.matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }

        // 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();    // 创建 QueryWrapper 对象，用于构建查询条件
        queryWrapper.eq("userAccount", userAccount);    // 添加查询条件：userAccount 字段等于 userAccount 变量的值
        queryWrapper.eq("userPassword", encryptPassword);    // 添加查询条件：userPassword 字段等于 encryptPassword
        User user = userMapper.selectOne(queryWrapper);     // 返符合条件的第一条数据
        // 用户不存在
        if (user == null) {
            log.info("账号和密码不匹配");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号和密码不匹配");
        }

        // 用户信息脱敏
        User safetyUser = getSafetyUser(user);
        // 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);

        return safetyUser;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setProfile(originUser.getProfile());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setUpdateTime(originUser.getUpdateTime());
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签列表查询用户
     *
     * @param tagNameList
     * @return
     */
    @Override
    public List<User> searchUserByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 第一种方法
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 拼接 and 查询， like "%java%" and like "%C++%"
        for (String tagName : tagNameList) {
            queryWrapper.like("tags", tagName);
        }
        List<User> userList = userMapper.selectList(queryWrapper);
        return userList.stream().map(user -> getSafetyUser(user)).collect(Collectors.toList());

        // 第二种方法，直接在内存中查，先从数据库中取出所有用户，再通过代码过滤出符合的用户
//        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
//        List<User> userList = userMapper.selectList(userQueryWrapper);
//       return userList.stream().filter(user -> {
//           if(user.getTags() == null){
//               return false;
//           }
//            String tagsStr = user.getTags();
//            Gson gson = new Gson();
//            Set<String> userTags = gson.fromJson(tagsStr, new TypeToken<Set<String>>(){}.getType());
//             userTags = Optional.ofNullable(userTags).orElse(new HashSet<>());  // 这个方法校验 userTags 如果为null，则给个默认值为 new HashSet<>()
//           // 检查这个用户的tags是否都在传入的tags中
//            for (String tagName : tagNameList) {
//                // 用户的tags数组不包含传入的tag的话则返回false，这个相当于includes
//                if(!userTags.contains(tagName)){
//                    return false;
//                }
//            }
//            return true;
//        }).collect(Collectors.toList());
    }

    /**
     * @param user      要修改用户
     * @param loginUser 当前登录用户
     * @return
     */
    @Override
    public int updateUser(User user, User loginUser) {

        Long userId = user.getId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }

        // 如果是管理，则可以修改任意用户
        // 如果不是管理，只能修改自己的信息
        if (!isAdmin(loginUser) && loginUser.getId() != user.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return userMapper.updateById(user);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return true-是 false-否
     */
    public boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    public boolean isAdmin(User user) {
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    public User getLoginUser(HttpServletRequest request) {
        Object loginUser = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return (User) loginUser;
    }

    /**
     * 匹配用户
     *
     * @param num
     * @param loginUser
     * @return
     */
    @Override
    public List<User> matchUsers(Long num, User loginUser) {
        String loginUserTags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> loginUserTagList = gson.fromJson(loginUserTags, new TypeToken<List<String>>() {
        }.getType());
        loginUserTagList = Optional.ofNullable(loginUserTagList).orElse(new ArrayList<>());

        // 获取所有用户
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.select("id", "tags");
        userQueryWrapper.isNotNull("tags");
        List<User> userList = this.list(userQueryWrapper);

        List<Pair<User, Long>> pairList = new ArrayList<>();

        // 遍历所有用户标签，取出每个用户的标签与登录用户进行比较相识度
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            long userId = loginUser.getId();
            if (StringUtils.isBlank(userTags) || userId == user.getId()) {
                continue;
            }

            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            // 计算分数
            long distance = AlgorithmUtil.minDistance(loginUserTagList, userTagList);
            pairList.add(new Pair<>(user, distance));
        }

        // 按编辑距离由小到大排序
        List<Pair<User, Long>> topPairUserList = pairList.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        List<Long> userIdList = topPairUserList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());

        userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id",userIdList);
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper).stream()
                .map(user -> getSafetyUser(user))
                .collect(Collectors.groupingBy(User::getId));

        // 重新排序
        ArrayList<User> finalUserList = new ArrayList<>();
        userIdList.forEach(userId -> finalUserList.add(userIdUserListMap.get(userId).get(0)));
        return finalUserList;

    }
}




