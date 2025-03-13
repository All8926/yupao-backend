package com.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mapper.TagMapper;
import com.example.model.domain.Tag;
import com.example.service.TagService;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2025-02-23 17:13:10
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService {

}




