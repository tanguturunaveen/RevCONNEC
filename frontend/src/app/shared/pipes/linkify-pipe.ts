import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({
  name: 'linkify',
  standalone: true
})
export class LinkifyPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) { }

  transform(content: string): SafeHtml {
    if (!content) return '';
    const urlRegex = /(https?:\/\/[^\s]+)/g;
    const html = content.replace(urlRegex, (url) => {
      return `<a href="${url}" target="_blank" rel="noopener noreferrer" class="msg-link">${url}</a>`;
    });
    return this.sanitizer.bypassSecurityTrustHtml(html);
  }
}
