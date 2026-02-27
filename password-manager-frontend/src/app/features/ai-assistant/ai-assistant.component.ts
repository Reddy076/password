import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="content-padding">
      <div class="page-header">
        <h1 class="page-title">AI Assistant</h1>
      </div>
      <div class="empty-state">
        <div class="empty-icon">🚧</div>
        <div class="empty-title">Coming Soon</div>
        <div class="empty-message">This feature is being implemented.</div>
      </div>
    </div>
  `,
  styles: []
})
export class AiAssistantComponent {}
