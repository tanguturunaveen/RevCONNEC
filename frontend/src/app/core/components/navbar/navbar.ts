import { Component, OnInit, OnDestroy, ChangeDetectorRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { NotificationService, NotificationResponse } from '../../services/notification.service';
import { MessageService } from '../../services/message.service';
import { AuthService } from '../../services/auth.service';
import { UserService, UserResponse } from '../../services/user.service';
import { FormsModule } from '@angular/forms';
import { interval, Subscription } from 'rxjs';
import { BottomNav } from '../bottom-nav/bottom-nav';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, BottomNav],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css']
})
export class Navbar implements OnInit, OnDestroy {
  unreadNotificationCount = 0;
  unreadMessageCount = 0;
  searchQuery = '';

  notificationDropdownOpen = false;
  notifications: NotificationResponse[] = [];
  notificationsLoading = false;

  isLightMode = false;
  currentUser: UserResponse | null = null;

  private pollSub?: Subscription;

  constructor(
    private notificationService: NotificationService,
    private messageService: MessageService,
    private authService: AuthService,
    private userService: UserService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit() {
    this.checkTheme();
    this.loadCounts();
    this.loadUser();

    // Notification count subscription (Once)
    this.notificationService.unreadCount$.subscribe(count => {
      this.unreadNotificationCount = count;
      this.cdr.markForCheck();
    });
    this.notificationService.refreshUnreadCount();

    // Message count subscription (reactive)
    this.messageService.unreadCount$.subscribe(count => {
      this.unreadMessageCount = count;
      this.cdr.markForCheck();
    });
    this.messageService.refreshUnreadCount();

    // Poll counts every 30 seconds
    this.pollSub = interval(30000).subscribe(() => this.loadCounts());
  }

  loadUser() {
    this.userService.getMyProfile().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.currentUser = res.data;
          this.cdr.markForCheck();
        }
      }
    });
  }

  checkTheme() {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'light') {
      this.isLightMode = true;
      document.body.classList.add('light-theme');
    }
  }

  toggleTheme() {
    this.isLightMode = !this.isLightMode;
    if (this.isLightMode) {
      document.body.classList.add('light-theme');
      localStorage.setItem('theme', 'light');
    } else {
      document.body.classList.remove('light-theme');
      localStorage.setItem('theme', 'dark');
    }
    this.cdr.markForCheck();
  }

  ngOnDestroy() {
    this.pollSub?.unsubscribe();
  }

  loadCounts() {
    this.messageService.refreshUnreadCount();
    this.notificationService.refreshUnreadCount();
  }

  toggleNotifications() {
    this.notificationDropdownOpen = !this.notificationDropdownOpen;
    if (this.notificationDropdownOpen && this.notifications.length === 0) {
      this.loadNotifications();
    }
  }

  loadNotifications() {
    this.notificationsLoading = true;
    this.cdr.markForCheck();

    this.notificationService.getNotifications(0, 10).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.notifications = res.data.content.map((n: any) => ({
            ...n,
            isRead: n.read !== undefined ? n.read : !!n.isRead
          }));
        }
        this.notificationsLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.notificationsLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  markAsRead(notification: NotificationResponse, event: Event) {
    event.stopPropagation();
    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.id).subscribe({
        next: () => {
          notification.isRead = true;
          this.unreadNotificationCount = Math.max(0, this.unreadNotificationCount - 1);
          this.cdr.markForCheck();
        }
      });
    }
  }

  navigateNotification(notification: NotificationResponse, event: Event) {
    event.stopPropagation();
    // Mark as read
    if (!notification.isRead) {
      notification.isRead = true;
      this.unreadNotificationCount = Math.max(0, this.unreadNotificationCount - 1);
      this.cdr.markForCheck();
      this.notificationService.markAsRead(notification.id).subscribe();
    }
    this.notificationDropdownOpen = false;

    // Navigate based on type
    const type: string = (notification as any).type || '';
    switch (type) {
      case 'LIKE':
      case 'COMMENT':
      case 'SHARE':
        this.router.navigate(['/feed']);
        break;
      case 'CONNECTION_REQUEST':
        this.router.navigate(['/profile'], { queryParams: { tab: 'requests' } });
        break;
      case 'FOLLOW':
      case 'NEW_FOLLOWER':
      case 'CONNECTION_ACCEPTED':
        const actorId = (notification as any).actorId || (notification as any).senderId;
        if (actorId) {
          this.router.navigate(['/profile', actorId]);
        } else {
          this.router.navigate(['/notifications']);
        }
        break;
      default:
        this.router.navigate(['/notifications']);
        break;
    }
  }

  markAllRead() {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.forEach(n => n.isRead = true);
        this.unreadNotificationCount = 0;
        this.cdr.markForCheck();
      }
    });
  }

  deleteNotification(notification: NotificationResponse, event: Event) {
    event.stopPropagation();
    this.notificationService.deleteNotification(notification.id).subscribe({
      next: () => {
        this.notifications = this.notifications.filter(n => n.id !== notification.id);
        if (!notification.isRead) {
          this.unreadNotificationCount = Math.max(0, this.unreadNotificationCount - 1);
        }
        this.cdr.markForCheck();
      }
    });
  }

  getNotificationIcon(type: string): string {
    const icons: Record<string, string> = {
      'LIKE': 'fa-heart',
      'COMMENT': 'fa-comment',
      'FOLLOW': 'fa-user-plus',
      'SHARE': 'fa-share-nodes',
      'CONNECTION_REQUEST': 'fa-user-clock',
      'CONNECTION_ACCEPTED': 'fa-user-check',
      'MESSAGE': 'fa-envelope'
    };
    return icons[type] || 'fa-bell';
  }

  getNotificationIconColor(type: string): string {
    const colors: Record<string, string> = {
      'LIKE': '#ef4444',
      'COMMENT': '#8b5cf6',
      'FOLLOW': '#8b5cf6',
      'SHARE': '#10b981',
      'CONNECTION_REQUEST': '#f59e0b',
      'CONNECTION_ACCEPTED': '#10b981',
      'MESSAGE': '#06b6d4'
    };
    return colors[type] || '#64748b';
  }

  getRelativeTime(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const seconds = Math.max(0, Math.floor((now.getTime() - date.getTime()) / 1000));
    if (seconds < 60) return 'just now';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return minutes + 'm';
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return hours + 'h';
    const days = Math.floor(hours / 24);
    return days + 'd';
  }

  onSearch() {
    if (this.searchQuery.trim()) {
      this.router.navigate(['/explore'], { queryParams: { q: this.searchQuery.trim() } });
      this.searchQuery = ''; // Clear after search
    }
  }

  navigateTo(route: string) {
    this.router.navigate([route]);
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  // Close dropdown when clicking outside
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event) {
    const target = event.target as HTMLElement;
    if (!target.closest('.notification-btn-wrapper')) {
      this.notificationDropdownOpen = false;
      this.cdr.markForCheck();
    }
  }
}
