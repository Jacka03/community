package com.community.event;



import com.alibaba.fastjson.JSONObject;
import com.community.entity.DiscussPost;
import com.community.entity.Event;
import com.community.entity.Message;
import com.community.service.DiscussPostService;
import com.community.service.ElasticsearchService;
import com.community.service.MessageService;
import com.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Component
public class EventConsumer implements CommunityConstant {
    // 处理事件，发送消息

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    // 一个方法消费多个主题
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_FOLLOW, TOPIC_LIKE})
    public void handleCommentMessage(ConsumerRecord record) {
        if(record == null || record.value() == null) {
            logger.error("消息内容为空");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if(event == null) {
            logger.error("消息格式错误");
            return;
        }

        // 发送站内通知
        Message message = new Message();
        // 消息构造者
        message.setFromId(SYSTEM_USER_ID);
        // 消息接收者
        message.setToId(event.getEntityUserId());
        // conversation存储主题，不在存储fromId_toId
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        // 包含：评论详情，事件是谁发起的，对哪个实体，
        Map<String, Object> content = new HashMap<>();
        // 事件是谁触发的
        content.put("userId", event.getUserId());
        // 实体的类型，对哪个实体做出处理：帖子，评论...
        content.put("entityType", event.getEntityType());
        // 实体的id
        content.put("entityId", event.getEntityId());

        if(!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entity : event.getData().entrySet()) {
                content.put(entity.getKey(), entity.getValue());
            }
        }

        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }

    // 消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);
    }

}
