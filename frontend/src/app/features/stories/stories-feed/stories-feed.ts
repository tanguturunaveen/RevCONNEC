import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StoryService, StoryResponse } from '../../../core/services/story.service';
import { MediaService } from '../../../core/services/media.service';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { UserService, UserResponse } from '../../../core/services/user.service';

@Component({
  selector: 'app-stories-feed',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './stories-feed.html',
  styleUrls: ['./stories-feed.css']
})
export class StoriesFeed implements OnInit {
  stories: StoryResponse[] = [];
  myStories: StoryResponse[] = [];
  currentUser: UserResponse | null = null;
  isLoading = false;

  // Create Story Modal active state
  showCreateModal = false;
  selectedMediaFile: File | null = null;
  mediaPreviewUrl: string | null = null;
  newStoryCaption = '';
  isCreating = false;
  isUploadingMedia = false;

  // View Story Modal
  activeStoryToView: StoryResponse | null = null;

  constructor(
    private storyService: StoryService,
    private mediaService: MediaService,
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit() {
    this.userService.getMyProfile().subscribe(res => {
      if (res.success) this.currentUser = res.data;
    });
    this.loadStories();
  }

  loadStories() {
    this.isLoading = true;
    this.storyService.getStoriesFeed().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.stories = res.data || [];
        }
        // Also fetch my stories separately to show "Add to your story" correctly
        this.fetchMyStories();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  fetchMyStories() {
    this.storyService.getMyStories().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.myStories = res.data || [];
        }
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  openCreateModal() {
    this.showCreateModal = true;
    this.selectedMediaFile = null;
    this.mediaPreviewUrl = null;
    this.newStoryCaption = '';
    this.cdr.markForCheck();
  }

  closeCreateModal() {
    this.showCreateModal = false;
    this.selectedMediaFile = null;
    this.mediaPreviewUrl = null;
    this.cdr.markForCheck();
  }

  triggerFileInput(fileInput: HTMLInputElement) {
    fileInput.click();
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

  createStory() {
    if (!this.selectedMediaFile) return;

    this.isCreating = true;
    this.isUploadingMedia = true;
    this.cdr.markForCheck();

    // Handle video or general file upload
    const uploadAction = this.selectedMediaFile.type.startsWith('video/')
      ? this.mediaService.uploadVideo(this.selectedMediaFile)
      : this.mediaService.uploadFile(this.selectedMediaFile);

    uploadAction.subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.publishStory(res.data.url);
        } else {
          this.isCreating = false;
          this.isUploadingMedia = false;
          this.cdr.markForCheck();
        }
      },
      error: (err) => {
        console.error('Error uploading story media:', err);
        this.isCreating = false;
        this.isUploadingMedia = false;
        alert('Failed to upload media. Please try again.');
        this.cdr.markForCheck();
      }
    });
  }

  private publishStory(mediaUrl: string) {
    this.storyService.createStory(mediaUrl, this.newStoryCaption).subscribe({
      next: (res) => {
        this.isCreating = false;
        this.isUploadingMedia = false;
        this.closeCreateModal();
        this.loadStories(); // Refresh feed
      },
      error: (err) => {
        this.isCreating = false;
        this.isUploadingMedia = false;
        alert(err?.error?.message || 'Failed to create story');
        this.cdr.markForCheck();
      }
    });
  }

  viewStory(story: StoryResponse) {
    this.activeStoryToView = story;
    this.cdr.markForCheck();
    // Call backend to increment view count
    this.storyService.viewStory(story.id).subscribe();
  }

  closeStoryView() {
    this.activeStoryToView = null;
    this.cdr.markForCheck();
  }

  get isMyActiveStory(): boolean {
    if (!this.activeStoryToView) return false;
    return this.myStories.some(s => s.id === this.activeStoryToView?.id);
  }

  deleteStory() {
    if (!this.activeStoryToView) return;
    if (!confirm('Are you sure you want to delete this story?')) return;

    this.storyService.deleteStory(this.activeStoryToView.id).subscribe({
      next: () => {
        this.closeStoryView();
        this.loadStories();
      },
      error: (err) => {
        alert(err?.error?.message || 'Failed to delete story');
      }
    });
  }
}
