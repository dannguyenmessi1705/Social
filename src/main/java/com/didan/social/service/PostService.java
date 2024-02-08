package com.didan.social.service;

import com.didan.social.dto.*;
import com.didan.social.entity.*;
import com.didan.social.entity.keys.PostLikeId;
import com.didan.social.entity.keys.UserPostId;
import com.didan.social.payload.request.CreatePostRequest;
import com.didan.social.payload.request.EditPostRequest;
import com.didan.social.repository.*;
import com.didan.social.service.impl.PostServiceImpl;
import com.didan.social.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostService implements PostServiceImpl {
    private final PostRepository postRepository;
    private final UserPostRepository userPostRepository;
    private final FileUploadsService fileUploadsService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserCommentRepository userCommentRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final HttpServletRequest request;
    @Autowired
    public PostService(PostRepository postRepository,
                       UserPostRepository userPostRepository,
                       FileUploadsService fileUploadsService,
                       JwtUtils jwtUtils,
                       UserRepository userRepository,
                       PostLikeRepository postLikeRepository,
                       UserCommentRepository userCommentRepository,
                       CommentRepository commentRepository,
                       CommentLikeRepository commentLikeRepository,
                       HttpServletRequest request
    ){
        this.postRepository = postRepository;
        this.userPostRepository = userPostRepository;
        this.fileUploadsService = fileUploadsService;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.postLikeRepository = postLikeRepository;
        this.userCommentRepository = userCommentRepository;
        this.commentRepository =commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.request = request;
    }
    @Override
    public String createPost(CreatePostRequest createPostRequest) throws Exception {
        String accessToken = jwtUtils.getTokenFromHeader(request);
        if (!StringUtils.hasText(accessToken)) throw new Exception("Not Authorized");
        if (!StringUtils.hasText(createPostRequest.getTitle())
            || !StringUtils.hasText(createPostRequest.getBody())
        ){
            throw new Exception("Miss some fields");
        }
        String email = jwtUtils.getEmailUserFromAccessToken(accessToken);
        if (email == null) throw new Exception("Have some errors");
        Users user = userRepository.findFirstByEmail(email);
        if (user == null) throw new Exception("User is not found");
        Posts post = new Posts();
        UserPosts userPost = new UserPosts();
        UUID postId = UUID.randomUUID();
        post.setPostId(postId.toString());
        post.setTitle(createPostRequest.getTitle());
        if (createPostRequest.getPostImg() != null && !createPostRequest.getPostImg().isEmpty()){
            String fileName = fileUploadsService.storeFile(createPostRequest.getPostImg(), "post", postId.toString());
            post.setPostImg("post/"+fileName);
        }
        post.setBody(createPostRequest.getBody());
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Date nowSql = Timestamp.valueOf(now);
        post.setPostedAt(nowSql);
        userPost.setUserPostId(new UserPostId(postId.toString(), user.getUserId()));
        postRepository.save(post);
        userPostRepository.save(userPost);
        return postId.toString();
    }

    @Override
    public List<PostDTO> getAllPosts() throws Exception {
        Sort sortByPostedAt = Sort.by(Sort.Direction.DESC, "postedAt");
        List<Posts> posts = postRepository.findAll(sortByPostedAt);
        if (posts.size() <= 0) throw new Exception("No posts are here");
        List<PostDTO> postDTOs = new ArrayList<>();
        for (Posts post : posts){
            UserPosts userPost = userPostRepository.findFirstByPosts(post);
            PostDTO postDTO = new PostDTO();
            postDTO.setPostId(post.getPostId());
            postDTO.setUserCreatedPost(userPost.getUserPostId().getUserId());
            postDTO.setTitle(post.getTitle());
            postDTO.setPostImg(post.getPostImg());
            postDTO.setBody(post.getBody());
            postDTO.setPostedAt(post.getPostedAt());
            List<PostLikes> postLikes = postLikeRepository.findByPosts_PostId(post.getPostId());
            // == Set<PostLikes> postLikes = post.getPostLike()
            List<String> userLikedPosts = postLikes.stream().map(postLike -> postLike.getUsers().getUserId()).collect(Collectors.toList());
            postDTO.setUserLikedPost(userLikedPosts);
            postDTO.setLikesQuantity(postLikes.size());
            List<UserComment> userComments = userCommentRepository.findByPosts_PostId(post.getPostId());
            postDTO.setCommentsQuantity(userComments.size());
            List<CommentDTO> commentDTOs = new ArrayList<>();
            for (UserComment userComment : userComments){
                Comments comment = commentRepository.findByCommentId(userComment.getComments().getCommentId());
                CommentDTO commentDTO = new CommentDTO();
                commentDTO.setCommentId(comment.getCommentId());
                commentDTO.setUserComments(userComment.getUsers().getUserId());
                commentDTO.setContent(comment.getContent());
                commentDTO.setCommentImg(comment.getCommentImg());
                commentDTO.setCommentAt(comment.getCommentAt());
                List<CommentLikes> commentLikes = commentLikeRepository.findAllByComments_CommentId(comment.getCommentId());
                commentDTO.setCommentLikes(commentLikes.size());
                List<String> userLikes = commentLikes.stream().map(commentLike -> commentLike.getUsers().getUserId()).collect(Collectors.toList());
                commentDTO.setUserLikes(userLikes);
                commentDTOs.add(commentDTO);
            }
            Collections.sort(commentDTOs, Comparator.comparing(CommentDTO::getCommentAt).reversed());
            postDTO.setComments(commentDTOs);
            postDTOs.add(postDTO);
        }
        return postDTOs;
    }

    @Override
    public PostDTO getPostById(String postId) throws Exception {
        Posts post = postRepository.findFirstByPostId(postId);
        if (post == null) throw new Exception("No post is here");
        PostDTO postDTO = new PostDTO();
        postDTO.setPostId(post.getPostId());
        UserPosts userPost = userPostRepository.findFirstByPosts(post);
        postDTO.setUserCreatedPost(userPost.getUserPostId().getUserId());
        postDTO.setTitle(post.getTitle());
        postDTO.setPostImg(post.getPostImg());
        postDTO.setBody(post.getBody());
        postDTO.setPostedAt(post.getPostedAt());
        List<PostLikes> postLikes = postLikeRepository.findByPosts_PostId(post.getPostId());
        List<String> userLikedPosts = postLikes.stream().map(postLike -> postLike.getUsers().getUserId()).collect(Collectors.toList());
        postDTO.setUserLikedPost(userLikedPosts);
        postDTO.setLikesQuantity(postLikes.size());
        List<UserComment> userComments = userCommentRepository.findByPosts_PostId(post.getPostId());
        postDTO.setCommentsQuantity(userComments.size());
        List<CommentDTO> commentDTOs = new ArrayList<>();
        for (UserComment userComment : userComments){
            Comments comment = commentRepository.findByCommentId(userComment.getComments().getCommentId());
            CommentDTO commentDTO = new CommentDTO();
            commentDTO.setCommentId(comment.getCommentId());
            commentDTO.setUserComments(userComment.getUsers().getUserId());
            commentDTO.setContent(comment.getContent());
            commentDTO.setCommentImg(comment.getCommentImg());
            commentDTO.setCommentAt(comment.getCommentAt());
            List<CommentLikes> commentLikes = commentLikeRepository.findAllByComments_CommentId(comment.getCommentId());
            commentDTO.setCommentLikes(commentLikes.size());
            List<String> userLikes = commentLikes.stream().map(commentLike -> commentLike.getUsers().getUserId()).collect(Collectors.toList());
            commentDTO.setUserLikes(userLikes);
            commentDTOs.add(commentDTO);
        }
        Collections.sort(commentDTOs, Comparator.comparing(CommentDTO::getCommentAt).reversed());
        postDTO.setComments(commentDTOs);
        return postDTO;
    }

    @Override
    public List<PostDTO> getPostByTitle(String searchName) throws Exception {
        List<Posts> posts = postRepository.findByTitleOrBodyContainingOrderByPostedAtDesc(searchName, searchName);
        if (posts.size() <= 0) throw new Exception("No posts are here");
        List<PostDTO> postDTOs = new ArrayList<>();
        for (Posts post : posts){
            PostDTO postDTO = new PostDTO();
            postDTO.setPostId(post.getPostId());
            UserPosts userPost = userPostRepository.findFirstByPosts(post);
            postDTO.setUserCreatedPost(userPost.getUserPostId().getUserId());
            postDTO.setTitle(post.getTitle());
            postDTO.setPostImg(post.getPostImg());
            postDTO.setBody(post.getBody());
            postDTO.setPostedAt(post.getPostedAt());
            List<PostLikes> postLikes = postLikeRepository.findByPosts_PostId(post.getPostId());
            List<String> userLikedPosts = postLikes.stream().map(postLike -> postLike.getUsers().getUserId()).collect(Collectors.toList());
            postDTO.setUserLikedPost(userLikedPosts);
            postDTO.setLikesQuantity(postLikes.size());
            List<UserComment> userComments = userCommentRepository.findByPosts_PostId(post.getPostId());
            postDTO.setCommentsQuantity(userComments.size());
            List<CommentDTO> commentDTOs = new ArrayList<>();
            for (UserComment userComment : userComments){
                Comments comment = commentRepository.findByCommentId(userComment.getComments().getCommentId());
                CommentDTO commentDTO = new CommentDTO();
                commentDTO.setCommentId(comment.getCommentId());
                commentDTO.setUserComments(userComment.getUsers().getUserId());
                commentDTO.setContent(comment.getContent());
                commentDTO.setCommentImg(comment.getCommentImg());
                commentDTO.setCommentAt(comment.getCommentAt());
                List<CommentLikes> commentLikes = commentLikeRepository.findAllByComments_CommentId(comment.getCommentId());
                commentDTO.setCommentLikes(commentLikes.size());
                List<String> userLikes = commentLikes.stream().map(commentLike -> commentLike.getUsers().getUserId()).collect(Collectors.toList());
                commentDTO.setUserLikes(userLikes);
                commentDTOs.add(commentDTO);
            }
            Collections.sort(commentDTOs, Comparator.comparing(CommentDTO::getCommentAt).reversed());
            postDTO.setComments(commentDTOs);
            postDTOs.add(postDTO);
        }
        return postDTOs;
    }

    @Override
    public boolean likePost(String postId) throws Exception {
        String accessToken = jwtUtils.getTokenFromHeader(request);
        if (!StringUtils.hasText(accessToken)) throw new Exception("Not Authorized");
        String email = jwtUtils.getEmailUserFromAccessToken(accessToken);
        if (email == null) throw new Exception("Have some errors");
        Users user = userRepository.findFirstByEmail(email);
        if (user == null) throw new Exception("User is not found");
        Posts post = postRepository.findFirstByPostId(postId);
        if (post==null) throw new Exception("No post is here");
        PostLikes postLikes = postLikeRepository.findByPosts_PostIdAndUsers_UserId(postId, user.getUserId());
        if (postLikes != null) throw new Exception("User liked post and cannot do it twice");
        PostLikes newPostLike = new PostLikes();
        newPostLike.setPostLikeId(new PostLikeId(postId, user.getUserId()));
        newPostLike.setPosts(post);
        newPostLike.setUsers(user);
        postLikeRepository.save(newPostLike);
        return true;
    }

    @Override
    public boolean unlikePost(String postId) throws Exception {
        String accessToken = jwtUtils.getTokenFromHeader(request);
        if (!StringUtils.hasText(accessToken)) throw new Exception("Not Authorized");
        String email = jwtUtils.getEmailUserFromAccessToken(accessToken);
        if (email == null) throw new Exception("Have some errors");
        Users user = userRepository.findFirstByEmail(email);
        if (user == null) throw new Exception("User is not found");
        Posts post = postRepository.findFirstByPostId(postId);
        if (post==null) throw new Exception("No post is here");
        PostLikes postLikes = postLikeRepository.findByPosts_PostIdAndUsers_UserId(postId, user.getUserId());
        if (postLikes == null) throw new Exception("User hasn't liked post yet");
        postLikeRepository.delete(postLikes);
        return true;
    }

    @Override
    public PostDTO updatePost(String postId, EditPostRequest editPostRequest) throws Exception {
        String accessToken = jwtUtils.getTokenFromHeader(request);
        if (!StringUtils.hasText(accessToken)) throw new Exception("Not Authorized");
        String email = jwtUtils.getEmailUserFromAccessToken(accessToken);
        if (email == null) throw new Exception("Have some errors");
        Users user = userRepository.findFirstByEmail(email);
        if (user == null) throw new Exception("User is not found");
        UserPosts userPost = userPostRepository.findFirstByPosts_PostIdAndUsers_UserId(postId, user.getUserId());
        if (userPost == null) throw new Exception("The user hasn't this post or not authorized to edit this post");
        Posts post = userPost.getPosts();
        if (StringUtils.hasText(editPostRequest.getTitle())){
            post.setTitle(editPostRequest.getTitle());
        }
        if (StringUtils.hasText(editPostRequest.getBody())){
            post.setBody(editPostRequest.getBody());
        }
        if (editPostRequest.getPostImg()!= null && !editPostRequest.getPostImg().isEmpty()){
            String fileName = fileUploadsService.storeFile(editPostRequest.getPostImg(), "post", postId.toString());
            post.setPostImg("post/"+fileName);
        }
        postRepository.save(post);
        PostDTO postDTO = new PostDTO();
        postDTO.setPostId(post.getPostId());
        postDTO.setUserCreatedPost(userPost.getUsers().getUserId());
        postDTO.setTitle(post.getTitle());
        postDTO.setPostImg(post.getPostImg());
        postDTO.setBody(post.getBody());
        postDTO.setPostedAt(post.getPostedAt());
        List<PostLikes> postLikes = postLikeRepository.findByPosts_PostId(post.getPostId());
        List<String> userLikedPosts = postLikes.stream().map(postLike -> postLike.getUsers().getUserId()).collect(Collectors.toList());
        postDTO.setUserLikedPost(userLikedPosts);
        postDTO.setLikesQuantity(postLikes.size());
        List<UserComment> userComments = userCommentRepository.findByPosts_PostId(post.getPostId());
        postDTO.setCommentsQuantity(userComments.size());
        List<CommentDTO> commentDTOs = new ArrayList<>();
        for (UserComment userComment : userComments){
            Comments comment = commentRepository.findByCommentId(userComment.getComments().getCommentId());
            CommentDTO commentDTO = new CommentDTO();
            commentDTO.setCommentId(comment.getCommentId());
            commentDTO.setUserComments(userComment.getUsers().getUserId());
            commentDTO.setContent(comment.getContent());
            commentDTO.setCommentImg(comment.getCommentImg());
            commentDTO.setCommentAt(comment.getCommentAt());
            List<CommentLikes> commentLikes = commentLikeRepository.findAllByComments_CommentId(comment.getCommentId());
            commentDTO.setCommentLikes(commentLikes.size());
            List<String> userLikes = commentLikes.stream().map(commentLike -> commentLike.getUsers().getUserId()).collect(Collectors.toList());
            commentDTO.setUserLikes(userLikes);
            commentDTOs.add(commentDTO);
        }
        Collections.sort(commentDTOs, Comparator.comparing(CommentDTO::getCommentAt).reversed());
        postDTO.setComments(commentDTOs);
        return postDTO;
    }

    @Override
    public boolean deletePost(String postId) throws Exception {
        try{
            String accessToken = jwtUtils.getTokenFromHeader(request);
            if (!StringUtils.hasText(accessToken)) throw new Exception("Not Authorized");
            String email = jwtUtils.getEmailUserFromAccessToken(accessToken);
            if (email == null) throw new Exception("Have some errors");
            Users user = userRepository.findFirstByEmail(email);
            if (user == null) throw new Exception("User is not found");
            Posts post = postRepository.findFirstByPostId(postId);
            if(post == null) throw new Exception("There isnt post to delete");
            UserPosts userPosts = userPostRepository.findFirstByPosts_PostIdAndUsers_UserId(postId, user.getUserId());
            if (userPosts == null) throw new Exception("The user hasn't this post or not authorized to edit this post");
            userPostRepository.delete(userPosts);
            postRepository.delete(post);
            if(StringUtils.hasText(post.getPostImg())){
                fileUploadsService.deleteFile(post.getPostImg());
            }
            return true;
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }
    }
}