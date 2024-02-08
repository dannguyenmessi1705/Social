package com.didan.social.dto;

import com.didan.social.entity.CommentLikes;
import com.didan.social.entity.UserComment;
import jakarta.persistence.*;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class CommentDTO {
    private String userComments;
    private String commentId;
    private String content;
    private String commentImg;
    private Date commentAt;
    private int commentLikes;
    private List<String> userLikes;

    public CommentDTO() {
    }

    public CommentDTO(String commentId, String content, String commentImg, Date commentAt, String userComments, int commentLikes, List<String> userLikes) {
        this.commentId = commentId;
        this.content = content;
        this.commentImg = commentImg;
        this.commentAt = commentAt;
        this.userComments = userComments;
        this.commentLikes = commentLikes;
        this.userLikes = userLikes;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCommentImg() {
        return commentImg;
    }

    public void setCommentImg(String commentImg) {
        this.commentImg = commentImg;
    }

    public Date getCommentAt() {
        return commentAt;
    }

    public void setCommentAt(Date commentAt) {
        this.commentAt = commentAt;
    }

    public String getUserComments() {
        return userComments;
    }

    public void setUserComments(String userComments) {
        this.userComments = userComments;
    }

    public int getCommentLikes() {
        return commentLikes;
    }

    public void setCommentLikes(int commentLikes) {
        this.commentLikes = commentLikes;
    }

    public List<String> getUserLikes() {
        return userLikes;
    }

    public void setUserLikes(List<String> userLikes) {
        this.userLikes = userLikes;
    }
}