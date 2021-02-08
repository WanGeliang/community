package com.nowcoder.community.service;

import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    MessageMapper messageMapper;

    @Autowired
    SensitiveFilter sensitiveFilter;

    public List<Message> findConversations(int userId, int offset, int limit) {
        return messageMapper.selectConversations(userId, offset, limit);
    }

    public int findConversationCount(int userId) {
        return messageMapper.selectConversationCount(userId);
    }

    public List<Message> findLetters(String conversation, int offset, int limit) {
        return messageMapper.selectLetters(conversation, offset, limit);
    }

    public int findLetterCount(String conversation) {
        return messageMapper.selectLetterCount(conversation);
    }

    public int findLetterUnreadCount(int userId, String conversation) {
        return messageMapper.selectLetterUnreadCount(userId, conversation);
    }

    /**
     * 添加一条消息进数据库
     * @param message
     * @return
     */
    public int addMessage(Message message) {
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        message.setContent(sensitiveFilter.filter(message.getContent()));
        return messageMapper.insertMessage(message);
    }

    /**
     * 修改未读消息，设置为已读消息
     * @param ids
     * @return
     */
    public int readMessage(List<Integer> ids) {
        return messageMapper.updateStatus(ids, 1);
    }
}
