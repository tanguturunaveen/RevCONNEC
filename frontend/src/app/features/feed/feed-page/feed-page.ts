import { Component, OnInit, ChangeDetectorRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Navbar } from '../../../core/components/navbar/navbar';
import { Sidebar } from '../../../core/components/sidebar/sidebar';
import { FormsModule } from '@angular/forms';
import { PostService, PostResponse } from '../../../core/services/post.service';
import { InteractionService, CommentResponse } from '../../../core/services/interaction.service';
import { BookmarkService } from '../../../core/services/bookmark.service';
import { MediaService } from '../../../core/services/media.service';
import { StoriesFeed } from '../../stories/stories-feed/stories-feed';
import { AnalyticsService, AnalyticsOverview } from '../../../core/services/analytics.service';
import { UserService, UserResponse } from '../../../core/services/user.service';
import { SearchService, TrendingHashtag } from '../../../core/services/search.service';
import { HashtagTextComponent } from '../../../shared/components/hashtag-text/hashtag-text.component';
import { BottomNav } from '../../../core/components/bottom-nav/bottom-nav';
import { ConnectionService } from '../../../core/services/connection.service';
import { MessageService } from '../../../core/services/message.service';

@Component({
  selector: 'app-feed-page',
  standalone: true,
  imports: [CommonModule, RouterModule, Navbar, Sidebar, FormsModule, StoriesFeed, HashtagTextComponent, BottomNav],
  templateUrl: './feed-page.html',
  styleUrls: ['./feed-page.css']
})
export class FeedPage implements OnInit {
  posts: PostResponse[] = [];
  newPostContent = '';
  selectedMediaFile: File | null = null;
  mediaPreviewUrl: string | null = null;
  isUploadingMedia = false;
  isLoading = false;

  // Business Post Fields
  showBusinessTools = false;
  showScheduleTool = false;
  ctaLabelInput = '';
  ctaUrlInput = '';
  scheduleDateOnlyInput = '';
  scheduleHourInput = '';
  scheduleMinuteInput = '';
  scheduleAmPmInput = 'AM';
  isPromotionalInput = false;
  partnerNameInput = '';
  postCategoryInput: 'STANDARD' | 'ANNOUNCEMENT' | 'UPDATE' = 'STANDARD';
  productTagsInput = '';

  get minDateString(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  formatScheduleTime() {
    if (this.scheduleHourInput) {
      let h = parseInt(this.scheduleHourInput, 10);
      if (isNaN(h) || h < 1) h = 12;
      if (h > 12) h = 12;
      this.scheduleHourInput = String(h).padStart(2, '0');
    }

    if (this.scheduleMinuteInput) {
      let m = parseInt(this.scheduleMinuteInput, 10);
      if (isNaN(m) || m < 0) m = 0;
      if (m > 59) m = 59;
      this.scheduleMinuteInput = String(m).padStart(2, '0');
    }
  }

  focusNextIfFull(value: string, nextElement: HTMLInputElement) {
    if (value && value.length === 2) {
      nextElement.focus();
    }
  }

  // Like state: { postId -> true/false }
  likedMap: { [postId: number]: boolean } = {};

  // Comment panel state: { postId -> boolean (open) }
  commentOpenMap: { [postId: number]: boolean } = {};

  // Comments data per post: { postId -> CommentResponse[] }
  commentsMap: { [postId: number]: CommentResponse[] } = {};

  // New comment input per post
  newCommentMap: { [postId: number]: string } = {};

  // Loading state per comment section
  commentLoadingMap: { [postId: number]: boolean } = {};

  // Bookmark state: { postId -> true/false }
  bookmarkedMap: { [postId: number]: boolean } = {};

  analyticsOverview: AnalyticsOverview | null = null;
  isLoadingAnalytics = false;

  trendingTopics: TrendingHashtag[] = [];

  // Management State
  currentUser: UserResponse | null = null;
  postOptionsOpenMap: { [id: number]: boolean } = {};
  editingPostId: number | null = null;
  editPostContent: string = '';
  commentOptionsOpenMap: { [id: number]: boolean } = {};

  editingCommentId: number | null = null;
  editCommentContent: string = '';

  // Comment Likes & Replies
  commentLikedMap: { [commentId: number]: boolean } = {};
  repliesMap: { [commentId: number]: CommentResponse[] } = {};
  commentReplyOpenMap: { [commentId: number]: boolean } = {};
  commentReplyInputMap: { [commentId: number]: string } = {};
  commentRepliesLoadingMap: { [commentId: number]: boolean } = {};

  // Share Modal State
  shareModalOpen = false;
  sharePostId: number | null = null;
  followingUsers: { userId: number; username: string; name: string; profilePicture: string | null }[] = [];
  shareUserSearch = '';
  isSendingShare = false;
  shareSuccessMap: { [userId: number]: boolean } = {};

  constructor(
    private postService: PostService,
    private interactionService: InteractionService,
    private bookmarkService: BookmarkService,
    private mediaService: MediaService,
    private analyticsService: AnalyticsService,
    private userService: UserService,
    private searchService: SearchService,
    private connectionService: ConnectionService,
    private messageService: MessageService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.userService.getMyProfile().subscribe({
      next: (res) => {
        if (res.success) this.currentUser = res.data;
        this.loadFeed();
        if (this.currentUser?.userType === 'BUSINESS' || this.currentUser?.userType === 'CREATOR') {
          this.loadAnalytics();
        }
        this.loadTrending();
      }
    });
  }

  loadTrending() {
    this.searchService.getTrendingHashtags(5).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.trendingTopics = res.data;
          this.cdr.markForCheck();
        }
      }
    });
  }

  loadAnalytics() {
    this.isLoadingAnalytics = true;
    this.analyticsService.getOverview().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.analyticsOverview = res.data;
        }
        this.isLoadingAnalytics = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error loading analytics:', err);
        this.isLoadingAnalytics = false;
        this.cdr.markForCheck();
      }
    });
  }

  sortPosts() {
    const myId = this.currentUser?.id;
    this.posts = [...this.posts].sort((a, b) => {
      // 1. Pinned status — only the post author's own pins float to top
      const aPinned = a.pinned && a.authorId === myId;
      const bPinned = b.pinned && b.authorId === myId;
      if (aPinned && !bPinned) return -1;
      if (!aPinned && bPinned) return 1;

      // 2. Creation date (most recent first)
      const dateA = new Date(a.createdAt).getTime();
      const dateB = new Date(b.createdAt).getTime();
      return dateB - dateA;
    });
    this.cdr.markForCheck();
  }

  // Management logic
  togglePostOptions(postId: number) {
    // Close others
    const currentState = this.postOptionsOpenMap[postId];
    this.postOptionsOpenMap = {};
    this.commentOptionsOpenMap = {};
    this.postOptionsOpenMap[postId] = !currentState;
    this.cdr.markForCheck();
  }

  isPostAuthor(post: PostResponse): boolean {
    return this.currentUser?.id == post.authorId;
  }

  isCommentAuthor(comment: CommentResponse): boolean {
    return this.currentUser?.id == comment.userId;
  }

  canEditComment(comment: CommentResponse): boolean {
    return this.currentUser?.id == comment.userId;
  }

  canDeleteComment(comment: CommentResponse, post: PostResponse | null): boolean {
    if (!this.currentUser) return false;
    return this.currentUser.id == comment.userId || (post ? this.currentUser.id == post.authorId : false);
  }

  toggleCommentOptions(commentId: number) {
    const currentState = this.commentOptionsOpenMap[commentId];
    this.postOptionsOpenMap = {};
    this.commentOptionsOpenMap = {};
    this.commentOptionsOpenMap[commentId] = !currentState;
    this.cdr.markForCheck();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    // If not clicking on an ellipsis or a dropdown option, close all
    if (!target.closest('.options-btn') && !target.closest('.options-dropdown')) {
      this.postOptionsOpenMap = {};
      this.commentOptionsOpenMap = {};
      this.cdr.markForCheck();
    }
  }

  deletePost(postId: number) {
    if (confirm('Are you sure you want to delete this post?')) {
      this.postService.deletePost(postId).subscribe({
        next: (res) => {
          if (res.success) {
            this.posts = this.posts.filter(p => p.id !== postId);
            this.cdr.markForCheck();
          }
        }
      });
    }
  }

  editPost(post: PostResponse) {
    this.editingPostId = post.id;
    this.editPostContent = post.content;
    this.postOptionsOpenMap[post.id] = false;
  }

  savePostEdit(postId: number) {
    if (!this.editPostContent.trim()) return;
    this.postService.updatePost(postId, { content: this.editPostContent }).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const idx = this.posts.findIndex(p => p.id === postId);
          if (idx !== -1) {
            this.posts[idx] = res.data;
          }
          this.editingPostId = null;
          this.cdr.markForCheck();
        }
      }
    });
  }

  cancelEdit() {
    this.editingPostId = null;
    this.editPostContent = '';
  }

  togglePin(postId: number) {
    this.postService.togglePin(postId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const idx = this.posts.findIndex(p => p.id === postId);
          if (idx !== -1) {
            this.posts[idx] = res.data;
            this.sortPosts();
          }
          this.postOptionsOpenMap[postId] = false;
          this.cdr.markForCheck();
        }
      },
      error: (err) => {
        console.error('Failed to toggle pin:', err);
        this.postOptionsOpenMap[postId] = false;
        this.cdr.markForCheck();
      }
    });
  }

  deleteComment(commentId: number, postId: number) {
    if (confirm('Delete this comment?')) {
      this.interactionService.deleteComment(commentId).subscribe({
        next: (res) => {
          if (res.success) {
            this.commentsMap[postId] = this.commentsMap[postId].filter(c => c.id !== commentId);
            const post = this.posts.find(p => p.id === postId);
            if (post) post.commentCount--;
            this.cdr.markForCheck();
          }
        }
      });
    }
  }

  editComment(comment: CommentResponse, postId: number) {
    this.editingCommentId = comment.id;
    this.editCommentContent = comment.content;
  }

  saveCommentEdit(commentId: number, postId: number) {
    if (!this.editCommentContent.trim()) return;
    this.interactionService.updateComment(commentId, this.editCommentContent).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const idx = this.commentsMap[postId].findIndex(c => c.id === commentId);
          if (idx !== -1) this.commentsMap[postId][idx] = res.data;
          this.editingCommentId = null;
          this.cdr.markForCheck();
        }
      }
    });
  }

  cancelCommentEdit() {
    this.editingCommentId = null;
    this.editCommentContent = '';
  }

  loadFeed() {
    this.isLoading = true;
    this.cdr.markForCheck();

    this.postService.getPersonalizedFeed(0, 10).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.posts = response.data.content;
          this.sortPosts();
          this.posts.forEach(post => {
            this.checkLikeStatus(post.id);
            this.checkBookmarkStatus(post.id);
            // Record impression only on load
            this.postService.recordImpression(post.id).subscribe();
          });
        }
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error fetching feed:', err);
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  checkLikeStatus(postId: number) {
    this.interactionService.hasLikedPost(postId).subscribe({
      next: (res) => {
        if (res.success) {
          this.likedMap[postId] = !!res.data;
          this.cdr.markForCheck();
        }
      }
    });
  }

  checkBookmarkStatus(postId: number) {
    this.bookmarkService.isBookmarked(postId).subscribe({
      next: (res) => {
        if (res.success) {
          this.bookmarkedMap[postId] = !!res.data;
          this.cdr.markForCheck();
        }
      }
    });
  }

  toggleBookmark(postId: number) {
    const isBookmarked = this.bookmarkedMap[postId];
    this.bookmarkedMap[postId] = !isBookmarked; // Optimistic

    const action = isBookmarked
      ? this.bookmarkService.removeBookmark(postId)
      : this.bookmarkService.bookmarkPost(postId);

    action.subscribe({
      error: () => {
        this.bookmarkedMap[postId] = isBookmarked; // Revert on error
        this.cdr.markForCheck();
      }
    });
    this.cdr.markForCheck();
  }

  triggerFileInput(fileInput: HTMLInputElement) {
    fileInput.click();
  }

  toggleBusinessTools() {
    this.showBusinessTools = !this.showBusinessTools;
  }

  isCreator(): boolean {
    return this.currentUser?.userType === 'CREATOR';
  }

  toggleScheduleTool() {
    this.showScheduleTool = !this.showScheduleTool;
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedMediaFile = input.files[0];

      // Create preview
      const reader = new FileReader();
      reader.onload = (e) => {
        this.mediaPreviewUrl = e.target?.result as string;
        this.cdr.markForCheck();
      };
      reader.readAsDataURL(this.selectedMediaFile);
    }
  }

  removeSelectedMedia() {
    this.selectedMediaFile = null;
    this.mediaPreviewUrl = null;
    this.cdr.markForCheck();
  }

  createPost() {
    if (!this.newPostContent.trim() && !this.selectedMediaFile) return;

    if (this.selectedMediaFile) {
      this.isUploadingMedia = true;
      this.cdr.markForCheck();

      // Handle video or general file upload
      const uploadAction = this.selectedMediaFile.type.startsWith('video/')
        ? this.mediaService.uploadVideo(this.selectedMediaFile)
        : this.mediaService.uploadFile(this.selectedMediaFile);

      uploadAction.subscribe({
        next: (res) => {
          if (res.success && res.data) {
            this.publishPost(res.data.url);
          } else {
            console.error('Media upload returned unsuccessful response:', res);
            alert('Media upload failed: ' + (res.message || 'Unknown error'));
            this.isUploadingMedia = false;
            this.cdr.markForCheck();
          }
        },
        error: (err) => {
          console.error('Error uploading media:', err);
          alert('Error uploading media. Please check your connection.');
          this.isUploadingMedia = false;
          this.cdr.markForCheck();
        }
      });
    } else {
      this.publishPost();
    }
  }

  private publishPost(mediaUrl?: string) {
    let finalContent = this.newPostContent;

    // Encode CTA tags for Business/Creator users - add if inputs have values
    if (this.ctaLabelInput.trim() && this.ctaUrlInput.trim()) {
      finalContent += `\n[[CTA|${this.ctaLabelInput.trim()}|${this.ctaUrlInput.trim()}]]`;
    }

    if (this.isPromotionalInput && this.isCreator() && this.partnerNameInput.trim()) {
      finalContent += `\n[[PROMO|${this.partnerNameInput.trim()}]]`;
    }

    if (this.isCreator() && this.productTagsInput.trim()) {
      const cleanedTags = this.productTagsInput.split(',')
        .map(t => t.trim())
        .filter(t => t.length > 0)
        .join(',');
      if (cleanedTags) {
        finalContent += `\n[[TAGS|${cleanedTags}]]`;
      }
    }

    let postType: string;
    if (this.postCategoryInput !== 'STANDARD') {
      postType = this.postCategoryInput;
    } else {
      postType = mediaUrl ? (this.selectedMediaFile?.type.startsWith('video/') ? 'VIDEO' : 'IMAGE') : 'TEXT';
    }

    if ((this.showBusinessTools || this.showScheduleTool) && this.scheduleDateOnlyInput) {
      // Build ISO string from 12-hour inputs
      let hour = parseInt(this.scheduleHourInput, 10) || 12;
      if (this.scheduleAmPmInput === 'PM' && hour !== 12) {
        hour += 12;
      } else if (this.scheduleAmPmInput === 'AM' && hour === 12) {
        hour = 0;
      }
      const hourStr = String(hour).padStart(2, '0');
      const minStr = String(parseInt(this.scheduleMinuteInput, 10) || 0).padStart(2, '0');
      const timeIso = `${hourStr}:${minStr}:00`;

      // Send the exact local date+time the user picked (no UTC conversion)
      const localDate = new Date(this.scheduleDateOnlyInput + 'T' + timeIso);
      const yyyy = localDate.getFullYear();
      const MM = String(localDate.getMonth() + 1).padStart(2, '0');
      const dd = String(localDate.getDate()).padStart(2, '0');
      const hh = String(localDate.getHours()).padStart(2, '0');
      const mm = String(localDate.getMinutes()).padStart(2, '0');
      const ss = String(localDate.getSeconds()).padStart(2, '0');
      const publishAtIso = `${yyyy}-${MM}-${dd}T${hh}:${mm}:${ss}`;
      const request = {
        content: finalContent,
        postType: postType as any,
        mediaUrls: mediaUrl ? [mediaUrl] : undefined,
        publishAt: publishAtIso
      };
      this.postService.schedulePost(request).subscribe({
        next: (response) => {
          if (response.success) {
            alert('Post scheduled successfully for ' + localDate.toLocaleString());
            this.newPostContent = '';
            this.ctaLabelInput = '';
            this.ctaUrlInput = '';
            this.scheduleDateOnlyInput = '';
            this.scheduleHourInput = '';
            this.scheduleMinuteInput = '';
            this.scheduleAmPmInput = 'AM';
            this.isPromotionalInput = false;
            this.partnerNameInput = '';
            this.productTagsInput = '';
            this.postCategoryInput = 'STANDARD';
            this.showBusinessTools = false;
            this.showScheduleTool = false;
            this.removeSelectedMedia();
          }
          this.isUploadingMedia = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Error scheduling post:', err);
          const backendMsg = err.error?.message || err.message || 'Unknown error';
          alert(`Failed to schedule post: ${backendMsg}`);
          this.isUploadingMedia = false;
          this.cdr.markForCheck();
        }
      });
    } else {
      const request = {
        content: finalContent,
        postType: postType as any,
        mediaUrls: mediaUrl ? [mediaUrl] : undefined
      };

      this.postService.createPost(request).subscribe({
        next: (response) => {
          if (response.success && response.data) {
            this.posts.unshift(response.data);
            this.sortPosts();
            this.likedMap[response.data.id] = false;
            this.newPostContent = '';
            this.ctaLabelInput = '';
            this.ctaUrlInput = '';
            this.scheduleDateOnlyInput = '';
            this.scheduleHourInput = '';
            this.scheduleMinuteInput = '';
            this.scheduleAmPmInput = 'AM';
            this.isPromotionalInput = false;
            this.partnerNameInput = '';
            this.productTagsInput = '';
            this.postCategoryInput = 'STANDARD';
            this.showBusinessTools = false;
            this.showScheduleTool = false;
            this.removeSelectedMedia(); // Clear media
          } else {
            alert('Error: ' + (response.message || 'Failed to create post'));
          }
          this.isUploadingMedia = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Error creating post:', err);
          alert(`Failed to create post. Status: ${err.status}. Error: ${err.message || 'Unknown'}`);
          this.isUploadingMedia = false;
          this.cdr.markForCheck();
        }
      });
    }
  }

  toggleLike(postId: number) {
    const isLiked = this.likedMap[postId];
    const post = this.posts.find(p => p.id === postId);
    if (!post) return;

    // Optimistic update
    this.likedMap[postId] = !isLiked;
    post.likeCount = isLiked ? Math.max(0, post.likeCount - 1) : post.likeCount + 1;
    this.cdr.markForCheck();

    const action = isLiked
      ? this.interactionService.unlikePost(postId)
      : this.interactionService.likePost(postId);

    action.subscribe({
      error: () => {
        // Revert on error
        this.likedMap[postId] = isLiked;
        post.likeCount = isLiked ? post.likeCount + 1 : Math.max(0, post.likeCount - 1);
        this.cdr.markForCheck();
      }
    });
  }

  toggleComments(postId: number) {
    this.commentOpenMap[postId] = !this.commentOpenMap[postId];

    if (this.commentOpenMap[postId]) {
      // Record view when user intentionally opens comments
      this.postService.recordView(postId).subscribe();

      if (!this.commentsMap[postId]) {
        this.loadComments(postId);
      }
    }
    this.cdr.markForCheck();
  }

  loadComments(postId: number) {
    this.commentLoadingMap[postId] = true;
    this.cdr.markForCheck();

    this.interactionService.getComments(postId, 0, 10).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.commentsMap[postId] = res.data.content;
        }
        this.commentLoadingMap[postId] = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.commentLoadingMap[postId] = false;
        this.cdr.markForCheck();
      }
    });
  }

  submitComment(postId: number) {
    const content = (this.newCommentMap[postId] || '').trim();
    if (!content) return;

    this.interactionService.addComment(postId, content).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          if (!this.commentsMap[postId]) this.commentsMap[postId] = [];
          this.commentsMap[postId].unshift(res.data);
          this.newCommentMap[postId] = '';
          // Update comment count on post
          const post = this.posts.find(p => p.id === postId);
          if (post) post.commentCount++;
          this.cdr.markForCheck();
        }
      },
      error: (err) => console.error('Error adding comment:', err)
    });
  }

  toggleCommentLike(commentId: number, postId: number) {
    const comment = this.commentsMap[postId]?.find(c => c.id === commentId) ||
      Object.values(this.repliesMap).flat().find(c => c.id === commentId);
    if (!comment) return;

    const isLiked = comment.isLikedByCurrentUser;
    comment.isLikedByCurrentUser = !isLiked;
    comment.likeCount = isLiked ? Math.max(0, comment.likeCount - 1) : comment.likeCount + 1;
    this.cdr.markForCheck();

    const action = isLiked
      ? this.interactionService.unlikeComment(commentId)
      : this.interactionService.likeComment(commentId);

    action.subscribe({
      error: () => {
        comment.isLikedByCurrentUser = isLiked;
        comment.likeCount = isLiked ? comment.likeCount + 1 : Math.max(0, comment.likeCount - 1);
        this.cdr.markForCheck();
      }
    });
  }

  toggleReplies(commentId: number) {
    this.commentReplyOpenMap[commentId] = !this.commentReplyOpenMap[commentId];
    if (this.commentReplyOpenMap[commentId] && !this.repliesMap[commentId]) {
      this.loadReplies(commentId);
    }
    this.cdr.markForCheck();
  }

  loadReplies(commentId: number) {
    this.commentRepliesLoadingMap[commentId] = true;
    this.cdr.markForCheck();

    this.interactionService.getCommentReplies(commentId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.repliesMap[commentId] = res.data.content;
        }
        this.commentRepliesLoadingMap[commentId] = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.commentRepliesLoadingMap[commentId] = false;
        this.cdr.markForCheck();
      }
    });
  }

  submitReply(postId: number, commentId: number) {
    const content = (this.commentReplyInputMap[commentId] || '').trim();
    if (!content) return;

    this.interactionService.addComment(postId, content, commentId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          if (!this.repliesMap[commentId]) this.repliesMap[commentId] = [];
          this.repliesMap[commentId].push(res.data);
          this.commentReplyInputMap[commentId] = '';

          // Increment reply count on parent
          const parent = this.commentsMap[postId]?.find(c => c.id === commentId);
          if (parent) {
            parent.replyCount = (parent.replyCount || 0) + 1;
          }

          // Increment post comment count
          const post = this.posts.find(p => p.id === postId);
          if (post) post.commentCount++;

          this.cdr.markForCheck();
        }
      },
      error: (err) => console.error('Error adding reply:', err)
    });
  }

  openShareModal(postId: number) {
    this.sharePostId = postId;
    this.shareUserSearch = '';
    this.shareSuccessMap = {};
    this.sharePendingMap = {};
    this.followingUsers = [];
    this.shareModalOpen = true;
    const userId = this.currentUser?.id;
    if (!userId) return;
    this.connectionService.getFollowing(userId, 0, 100).subscribe({
      next: (res) => {
        if (res.success && res.data?.content) {
          this.followingUsers = res.data.content.map((c: any) => ({
            userId: c.userId,
            username: c.username,
            name: c.name,
            profilePicture: c.profilePicture
          }));
          this.cdr.markForCheck();
        }
      },
      error: (err) => console.error('Error fetching following users:', err)
    });
  }

  closeShareModal() {
    this.shareModalOpen = false;
    this.sharePostId = null;
  }

  get filteredShareUsers() {
    const q = this.shareUserSearch.toLowerCase();
    if (!q) return this.followingUsers;
    return this.followingUsers.filter(u =>
      u.username.toLowerCase().includes(q) || u.name.toLowerCase().includes(q)
    );
  }

  sharePendingMap: { [userId: number]: boolean } = {};

  sendPostToUser(recipientId: number) {
    if (!this.sharePostId || this.shareSuccessMap[recipientId] || this.sharePendingMap[recipientId]) return;
    this.sharePendingMap[recipientId] = true;
    this.cdr.markForCheck();

    const currentSharePostId = this.sharePostId;
    const post = this.posts.find(p => p.id === currentSharePostId);
    const postUrl = `${window.location.origin}/post/${currentSharePostId}`;

    // Build a structured shared post message
    const sharedData: any = { postId: currentSharePostId, postUrl };
    if (post) {
      sharedData.authorName = post.authorName || '';
      sharedData.authorUsername = post.authorUsername || '';
      sharedData.authorPic = post.authorProfilePicture || '';
      if (post.content) {
        const cleanContent = post.content
          .replace(/\[\[CTA\|.*?\|.*?\]\]/g, '')
          .replace(/\[\[PROMO\|.*?\]\]/g, '')
          .replace(/\[\[TAGS\|.*?\]\]/g, '')
          .replace(/\/uploads\/[^\s]+/g, '')
          .trim();
        if (cleanContent) {
          sharedData.content = cleanContent.length > 200 ? cleanContent.substring(0, 200) + '...' : cleanContent;
        }
      }
      if (post.mediaUrls && post.mediaUrls.length > 0) {
        sharedData.mediaUrl = post.mediaUrls[0];
      }
    }
    const message = `[[SHARED_POST:${JSON.stringify(sharedData)}]]`;

    // Optimistic UI update
    const postToUpdate = this.posts.find(p => p.id === currentSharePostId);
    if (postToUpdate) {
      postToUpdate.shareCount++;
      this.cdr.markForCheck();
    }

    this.messageService.createConversation(recipientId).subscribe({
      next: (res) => {
        const conversationId = res.data?.userId ?? recipientId;
        this.messageService.sendMessage(conversationId, message).subscribe({
          next: () => {
            this.shareSuccessMap[recipientId] = true;
            this.sharePendingMap[recipientId] = false;
            this.cdr.markForCheck();

            this.interactionService.incrementShareCount(currentSharePostId).subscribe({
              next: () => {},
              error: (err) => console.error('Error incrementing share count:', err)
            });
          },
          error: (err) => {
            console.error('Error sending share message:', err);
            this.sharePendingMap[recipientId] = false;
            if (postToUpdate) {
              postToUpdate.shareCount = Math.max(0, postToUpdate.shareCount - 1);
            }
            this.cdr.markForCheck();
          }
        });
      },
      error: (err) => {
        console.error('Error creating conversation:', err);
        this.sharePendingMap[recipientId] = false;
        if (postToUpdate) {
          postToUpdate.shareCount = Math.max(0, postToUpdate.shareCount - 1);
        }
        this.cdr.markForCheck();
      }
    });
  }

  // Keep legacy method for backward compatibility with template
  likePost(postId: number) {
    this.toggleLike(postId);
  }

  getRelativeTime(dateString: string): string {
    if (!dateString) return '';
    // Treat backend timestamps as UTC (server runs in UTC)
    const utcString = dateString.endsWith('Z') || dateString.includes('+') ? dateString : dateString + 'Z';
    const date = new Date(utcString);
    const now = new Date();
    const seconds = Math.max(0, Math.floor((now.getTime() - date.getTime()) / 1000));

    if (seconds < 60) return 'just now';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return minutes + 'm ago';
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return hours + 'h ago';
    const days = Math.floor(hours / 24);
    if (days < 7) return days + 'd ago';
    if (days < 30) return Math.floor(days / 7) + 'w ago';
    if (days < 365) return Math.floor(days / 30) + 'mo ago';
    return Math.floor(days / 365) + 'y ago';
  }
}


