package org.revature.revconnect.service;

import org.revature.revconnect.dto.response.BookmarkResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.model.Bookmark;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.BookmarkRepository;
import org.revature.revconnect.repository.PostRepository;
import org.revature.revconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostService postService;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public void bookmarkPost(Long postId) {
        User currentUser = getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (bookmarkRepository.existsByUserAndPost(currentUser, post)) {
            throw new BadRequestException("Post already bookmarked");
        }

        Bookmark bookmark = Bookmark.builder()
                .user(currentUser)
                .post(post)
                .build();

        bookmarkRepository.save(bookmark);
        log.info("User {} bookmarked post {}", currentUser.getUsername(), postId);
    }

    public void removeBookmark(Long postId) {
        User currentUser = getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        bookmarkRepository.deleteByUserAndPost(currentUser, post);
        log.info("User {} removed bookmark from post {}", currentUser.getUsername(), postId);
    }

    public PagedResponse<BookmarkResponse> getBookmarks(int page, int size) {
        User currentUser = getCurrentUser();
        Page<Bookmark> bookmarkPage = bookmarkRepository.findByUserOrderByCreatedAtDesc(
                currentUser, PageRequest.of(page, size));

        List<BookmarkResponse> content = bookmarkPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PagedResponse.<BookmarkResponse>builder()
                .content(content)
                .pageNumber(bookmarkPage.getNumber())
                .pageSize(bookmarkPage.getSize())
                .totalElements(bookmarkPage.getTotalElements())
                .totalPages(bookmarkPage.getTotalPages())
                .last(bookmarkPage.isLast())
                .build();
    }

    public boolean isBookmarked(Long postId) {
        User currentUser = getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return bookmarkRepository.existsByUserAndPost(currentUser, post);
    }

    private BookmarkResponse mapToResponse(Bookmark bookmark) {
        Post post = bookmark.getPost();
        PostResponse postResponse = postService.toResponseWithFullMetadata(post);

        return BookmarkResponse.builder()
                .id(bookmark.getId())
                .post(postResponse)
                .bookmarkedAt(bookmark.getCreatedAt())
                .build();
    }
}
