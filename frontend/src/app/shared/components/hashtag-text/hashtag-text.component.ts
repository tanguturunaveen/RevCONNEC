import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-hashtag-text',
  standalone: true,
  imports: [CommonModule],
  template: `
    <ng-container *ngFor="let part of parts">
      <ng-container *ngIf="part.type === 'text'">{{part.content}}</ng-container>
      <ng-container *ngIf="part.type === 'tag'">
        <a class="hashtag-link" (click)="goToTag(part.content, $event)">{{part.content}}</a>
      </ng-container>
      <ng-container *ngIf="part.type === 'url'">
        <a [href]="part.content" target="_blank" rel="noopener" class="url-link">{{part.content}}</a>
      </ng-container>
    </ng-container>
  `,
  styles: [`
    .hashtag-link {
      color: #8b5cf6;
      font-weight: 500;
      cursor: pointer;
      text-decoration: none;
    }
    .hashtag-link:hover {
      text-decoration: underline;
    }
    .url-link {
      color: #8b5cf6;
      word-break: break-all;
      text-decoration: none;
    }
    .url-link:hover {
      text-decoration: underline;
    }
  `]
})
export class HashtagTextComponent implements OnChanges {
  @Input() text: string = '';

  parts: { type: 'text' | 'tag' | 'url', content: string }[] = [];

  constructor(private router: Router) { }

  ngOnChanges() {
    this.parseText();
  }

  parseText() {
    if (!this.text) {
      this.parts = [];
      return;
    }

    // Clean internal tags like [[CTA|...]], [[PROMO|...]], [[TAGS|...]]
    let cleanText = this.text.replace(/\[\[CTA\|.*?\]\]/g, '')
      .replace(/\[\[PROMO\|.*?\]\]/g, '')
      .replace(/\[\[TAGS\|.*?\]\]/g, '')
      .trim();

    // Split by hashtags and URLs (http/https or /uploads paths)
    const regex = /(#[a-zA-Z0-9_]+|https?:\/\/[^\s]+|\/uploads\/[^\s]+)/g;
    const result: { type: 'text' | 'tag' | 'url', content: string }[] = [];
    let lastIndex = 0;
    let match;

    while ((match = regex.exec(cleanText)) !== null) {
      if (match.index > lastIndex) {
        result.push({ type: 'text', content: cleanText.slice(lastIndex, match.index) });
      }
      const token = match[0];
      if (token.startsWith('#')) {
        result.push({ type: 'tag', content: token });
      } else {
        result.push({ type: 'url', content: token });
      }
      lastIndex = regex.lastIndex;
    }
    if (lastIndex < cleanText.length) {
      result.push({ type: 'text', content: cleanText.slice(lastIndex) });
    }

    this.parts = result.filter(p => p.content.trim().length > 0);
  }

  goToTag(tag: string, event: Event) {
    event.stopPropagation();
    // navigate to explore with the tag
    this.router.navigate(['/explore'], { queryParams: { q: tag } });
  }
}
