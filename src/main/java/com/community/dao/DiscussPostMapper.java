package com.community.dao;

import com.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    /**
     * 分页查询
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    List<DiscussPost> selectDiscussPosts(@Param("userId") int userId,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /**
     * 查询帖子函数
     * @param userId
     * @return
     */
    int selectDiscussPostRows(@Param("userId") int userId);

    /**
     * 增加帖子
     */

    int insertDiscussPost(DiscussPost discussPost);

    /**
     * 根据id获取帖子
     * @param id 帖子id
     * @return
     */
    DiscussPost selectDiscussPostById(int id);

    /**
     * 更新帖子评论的数量
     * @param id 帖子id
     * @param commentCount 评论数量
     * @return
     */
    int updateCommentCount(int id, int commentCount);


    /**
     * 根据id修改帖子类型
     * @param id 帖子id
     * @param type 修改后的类型
     * @return
     */
    int updateType(int id, int type);

    /**
     * 根据id修改帖子状态
     * @param id 帖子id
     * @param status 修改后的状态
     * @return
     */
    int updateStatus(int id, int status);

}
