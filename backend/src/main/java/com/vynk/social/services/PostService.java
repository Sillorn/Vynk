package com.vynk.social.services;

import com.vynk.connections.repositories.AcceptedConnectionRepository;
import com.vynk.identity.entities.UserAccount;
import com.vynk.identity.repositories.UserAccountRepository;
import com.vynk.shared.api.PageQuery;
import com.vynk.shared.api.PageResponse;
import com.vynk.shared.error.ApiException;
import com.vynk.social.controllers.PostApi;
import com.vynk.social.entities.Post;
import com.vynk.social.entities.PostComment;
import com.vynk.social.entities.PostLike;
import com.vynk.social.entities.PostLikeId;
import com.vynk.social.repositories.PostCommentRepository;
import com.vynk.social.repositories.PostLikeRepository;
import com.vynk.social.repositories.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepository posts;
    private final UserAccountRepository accounts;
    private final PostCommentRepository comments;
    private final PostLikeRepository likes;
    private final AcceptedConnectionRepository connections;

    public PostService(PostRepository posts, UserAccountRepository accounts,
                       PostCommentRepository comments, PostLikeRepository likes,
                       AcceptedConnectionRepository connections) {
        this.posts = posts;
        this.accounts = accounts;
        this.comments = comments;
        this.likes = likes;
        this.connections = connections;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostApi.PostResponse> connectionsFeed(String username, PageQuery query) {
        UserAccount account = accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        int page = query.pageNumber();
        int size = query.pageSize();
        var result = posts.findConnectionsFeed(account.getId(), PageRequest.of(page, size, Sort.unsorted()));
        return PageResponse.of(result.getContent().stream().map(PostApi.PostResponse::from).toList(),
                page, size, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<PostApi.PostResponse> publicFeed(PageQuery query) {
        int page = query.pageNumber();
        int size = query.pageSize();
        var result = posts.findPublicFeed(PageRequest.of(page, size, Sort.unsorted()));
        return PageResponse.of(result.getContent().stream().map(PostApi.PostResponse::from).toList(),
                page, size, result.getTotalElements());
    }

    @Transactional
    public PostApi.PostResponse create(String username, PostApi.WriteRequest request) {
        UserAccount author = accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        Post post = posts.save(new Post(author, request.content().trim(), audience(request.audience())));
        return PostApi.PostResponse.from(post);
    }

    @Transactional
    public PostApi.PostResponse update(String username, long id, PostApi.WriteRequest request) {
        Post post = requireOwned(username, id);
        post.setContent(request.content().trim());
        // An omitted audience on legacy clients preserves the existing visibility.
        if (request.audience() != null) post.setAudience(audience(request.audience()));
        return PostApi.PostResponse.from(post);
    }

    @Transactional
    public void delete(String username, long id) {
        posts.delete(requireOwned(username, id));
    }

    @Transactional(readOnly = true)
    public PostApi.EngagementResponse engagement(long postId, String username) {
        UserAccount viewer = optionalAccount(username);
        Post post = requireVisible(postId, viewer);
        boolean liked = viewer != null && likes.existsById(new PostLikeId(post.getId(), viewer.getId()));
        return new PostApi.EngagementResponse(liked, likes.countByIdPostId(postId), comments.countByPostId(postId));
    }

    @Transactional
    public PostApi.LikeResponse toggleLike(String username, long postId) {
        UserAccount account = accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        Post post = requireVisible(postId, account);
        PostLikeId likeId = new PostLikeId(postId, account.getId());
        boolean liked;
        if (likes.existsById(likeId)) {
            likes.deleteById(likeId);
            liked = false;
        } else {
            likes.save(new PostLike(post, account));
            liked = true;
        }
        return new PostApi.LikeResponse(liked, likes.countByIdPostId(postId));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostApi.CommentResponse> comments(long postId, String username, PageQuery query) {
        requireVisible(postId, optionalAccount(username));
        var result = comments.findByPostIdOrderByCreatedAtAscIdAsc(postId,
                PageRequest.of(query.pageNumber(), query.pageSize(), Sort.unsorted()));
        return PageResponse.of(result.getContent().stream().map(PostApi.CommentResponse::from).toList(),
                query.pageNumber(), query.pageSize(), result.getTotalElements());
    }

    @Transactional
    public PostApi.CommentResponse addComment(String username, long postId, PostApi.CommentRequest request) {
        UserAccount author = accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        Post post = requireVisible(postId, author);
        return PostApi.CommentResponse.from(comments.save(
                new PostComment(post, author, request.content().trim())));
    }

    @Transactional
    public PostApi.CommentResponse updateComment(String username, long id, PostApi.CommentRequest request) {
        PostComment comment = comments.findOwned(id, username)
                .orElseThrow(() -> ApiException.notFound("Comment not found."));
        comment.setContent(request.content().trim());
        return PostApi.CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(String username, long id) {
        PostComment comment = comments.findOwned(id, username)
                .orElseThrow(() -> ApiException.notFound("Comment not found."));
        comments.delete(comment);
    }

    private static String audience(String value) { return value == null ? "PUBLIC" : value; }

    private Post requireOwned(String username, long id) {
        return posts.findOwned(id, username).orElseThrow(() -> ApiException.notFound("Post not found."));
    }

    /**
     * Loads a post the viewer may read or engage with. Connection-only posts are visible to the
     * author and to accepted connections of the author; everyone else (including anonymous
     * viewers) gets the same 404 as a missing post so existence is not revealed.
     */
    private Post requireVisible(long postId, UserAccount viewer) {
        Post post = posts.findById(postId).orElseThrow(() -> ApiException.notFound("Post not found."));
        if ("PUBLIC".equals(post.getAudience())) return post;
        if (viewer == null) throw ApiException.notFound("Post not found.");
        long authorId = post.getAuthor().getId();
        long viewerId = viewer.getId();
        if (authorId == viewerId) return post;
        long low = Math.min(authorId, viewerId);
        long high = Math.max(authorId, viewerId);
        if (!connections.existsPair(low, high)) throw ApiException.notFound("Post not found.");
        return post;
    }

    /** Null for anonymous callers; unknown usernames are a 404 like every other account lookup. */
    private UserAccount optionalAccount(String username) {
        if (username == null) return null;
        return accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
    }
}
