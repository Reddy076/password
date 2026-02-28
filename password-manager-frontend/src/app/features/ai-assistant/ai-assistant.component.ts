import { Component, inject, signal, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { UiStateService } from '../../core/state/ui.state';
import { AiChatResponse, AiMessage } from '../../core/models/security.models';

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="ai-layout">
      <!-- Header -->
      <div class="ai-header">
        <div class="ai-avatar">🤖</div>
        <div>
          <h2 class="ai-title">AI Security Assistant</h2>
          <p class="ai-subtitle">Ask me about passwords, security, and vault management</p>
        </div>
        <button class="pill-btn pill-btn-ghost pill-btn-sm" (click)="clearChat()">Clear Chat</button>
      </div>

      <!-- Messages -->
      <div class="messages-container" #messagesContainer>
        <!-- Welcome message -->
        <div class="message assistant-message" *ngIf="messages().length === 0">
          <div class="message-avatar">🤖</div>
          <div class="message-bubble">
            <p>Hello! I'm your AI security assistant. I can help you with:</p>
            <ul>
              <li>🔑 Generating strong passwords</li>
              <li>🛡️ Security analysis and recommendations</li>
              <li>🚨 Breach detection guidance</li>
              <li>📱 Two-factor authentication setup</li>
              <li>💡 Password management best practices</li>
            </ul>
            <p>What would you like to know?</p>
          </div>
        </div>

        <ng-container *ngFor="let msg of messages()">
          <div class="message" [class]="msg.role === 'user' ? 'user-message' : 'assistant-message'">
            <div class="message-avatar" *ngIf="msg.role === 'assistant'">🤖</div>
            <div class="message-bubble">
              <div class="message-text" [innerHTML]="formatMessage(msg.content)"></div>
              <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
            <div class="message-avatar user-avatar" *ngIf="msg.role === 'user'">👤</div>
          </div>
        </ng-container>

        <!-- Typing indicator -->
        <div class="message assistant-message" *ngIf="loading()">
          <div class="message-avatar">🤖</div>
          <div class="message-bubble typing-indicator">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <!-- Quick Suggestions -->
      <div class="suggestions" *ngIf="messages().length === 0">
        <button class="suggestion-chip" *ngFor="let s of suggestions" (click)="sendSuggestion(s)">
          {{ s }}
        </button>
      </div>

      <!-- Input -->
      <div class="ai-input-area">
        <div class="ai-input-wrapper">
          <textarea
            class="ai-input"
            placeholder="Ask me anything about security..."
            [(ngModel)]="inputMessage"
            (keydown.enter)="onEnterKey($event)"
            rows="1"
            #inputRef
          ></textarea>
          <button class="send-btn" (click)="sendMessage()" [disabled]="!inputMessage.trim() || loading()">
            {{ loading() ? '⏳' : '➤' }}
          </button>
        </div>
        <div class="input-hint">Press Enter to send, Shift+Enter for new line</div>
      </div>
    </div>
  `,
  styles: [`
    .ai-layout {
      display: flex;
      flex-direction: column;
      height: 100%;
      overflow: hidden;
    }

    .ai-header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 20px;
      border-bottom: 1px solid var(--border-subtle);
      flex-shrink: 0;
    }
    .ai-avatar {
      width: 44px; height: 44px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
      display: flex; align-items: center; justify-content: center;
      font-size: 22px; flex-shrink: 0;
    }
    .ai-title { font-size: 16px; font-weight: 700; color: var(--text-primary); margin: 0 0 2px; }
    .ai-subtitle { font-size: 12px; color: var(--text-muted); margin: 0; }

    .messages-container {
      flex: 1;
      overflow-y: auto;
      padding: 16px 20px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      scrollbar-width: thin;
      scrollbar-color: var(--border-default) transparent;
    }

    .message {
      display: flex;
      align-items: flex-end;
      gap: 8px;
      max-width: 85%;
    }
    .user-message {
      align-self: flex-end;
      flex-direction: row-reverse;
    }
    .assistant-message {
      align-self: flex-start;
    }

    .message-avatar {
      width: 32px; height: 32px;
      border-radius: 50%;
      background: var(--bg-elevated);
      display: flex; align-items: center; justify-content: center;
      font-size: 16px; flex-shrink: 0;
    }
    .user-avatar {
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
    }

    .message-bubble {
      padding: 10px 14px;
      border-radius: 18px;
      font-size: 13px;
      line-height: 1.6;
      max-width: 100%;
    }
    .user-message .message-bubble {
      background: var(--accent-primary);
      color: #fff;
      border-bottom-right-radius: 4px;
    }
    .assistant-message .message-bubble {
      background: var(--bg-elevated);
      color: var(--text-primary);
      border-bottom-left-radius: 4px;
      border: 1px solid var(--border-subtle);
    }
    .message-text {
      ul { padding-left: 16px; margin: 6px 0; }
      li { margin-bottom: 4px; }
      p { margin: 0 0 6px; &:last-child { margin: 0; } }
      code { font-family: var(--font-mono); background: rgba(0,0,0,0.2); padding: 1px 4px; border-radius: 4px; font-size: 12px; }
    }
    .message-time { font-size: 10px; opacity: 0.6; margin-top: 4px; }

    .typing-indicator {
      display: flex; align-items: center; gap: 4px; padding: 12px 16px;
      span {
        width: 8px; height: 8px; border-radius: 50%;
        background: var(--text-muted);
        animation: typing 1.4s infinite;
        &:nth-child(2) { animation-delay: 0.2s; }
        &:nth-child(3) { animation-delay: 0.4s; }
      }
    }
    @keyframes typing {
      0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
      30% { transform: translateY(-6px); opacity: 1; }
    }

    .suggestions {
      display: flex; flex-wrap: wrap; gap: 8px;
      padding: 0 20px 12px;
    }
    .suggestion-chip {
      padding: 6px 14px;
      border-radius: var(--pill-radius-full);
      background: var(--bg-elevated);
      border: 1px solid var(--border-default);
      cursor: pointer; font-size: 12px; color: var(--text-secondary);
      transition: all var(--transition-fast);
      &:hover { border-color: var(--accent-primary); color: var(--accent-primary); background: var(--bg-active); }
    }

    .ai-input-area {
      padding: 12px 20px 16px;
      border-top: 1px solid var(--border-subtle);
      flex-shrink: 0;
    }
    .ai-input-wrapper {
      display: flex; align-items: flex-end; gap: 8px;
      background: var(--bg-elevated);
      border: 1px solid var(--border-default);
      border-radius: var(--pill-radius-lg);
      padding: 8px 8px 8px 16px;
      transition: border-color var(--transition-fast);
      &:focus-within { border-color: var(--accent-primary); box-shadow: 0 0 0 3px rgba(99,102,241,0.15); }
    }
    .ai-input {
      flex: 1; background: none; border: none; outline: none;
      color: var(--text-primary); font-size: 13px; font-family: var(--font-sans);
      resize: none; max-height: 120px; line-height: 1.5;
      &::placeholder { color: var(--text-muted); }
    }
    .send-btn {
      width: 36px; height: 36px; border-radius: 50%;
      background: var(--accent-primary); border: none; cursor: pointer;
      display: flex; align-items: center; justify-content: center;
      font-size: 16px; color: #fff; flex-shrink: 0;
      transition: all var(--transition-fast);
      &:hover { background: #818cf8; transform: scale(1.05); }
      &:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
    }
    .input-hint { font-size: 10px; color: var(--text-muted); margin-top: 4px; text-align: right; }
  `]
})
export class AiAssistantComponent implements OnInit, AfterViewChecked {
  private http = inject(HttpClient);
  private uiState = inject(UiStateService);

  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  messages = signal<AiMessage[]>([]);
  loading = signal(false);
  inputMessage = '';
  private shouldScroll = false;

  suggestions = [
    '🔑 Generate a strong password for banking',
    '🛡️ How can I improve my security score?',
    '📱 How do I set up two-factor authentication?',
    '🚨 What should I do if my password is breached?',
    '💡 Best practices for password management',
    '🔄 Why should I avoid reusing passwords?'
  ];

  ngOnInit(): void {}

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  sendMessage(): void {
    const message = this.inputMessage.trim();
    if (!message || this.loading()) return;

    this.inputMessage = '';
    const userMsg: AiMessage = { role: 'user', content: message, createdAt: new Date().toISOString() };
    this.messages.update(msgs => [...msgs, userMsg]);
    this.shouldScroll = true;
    this.loading.set(true);

    this.http.post<AiChatResponse>('/api/ai/chat', { message }).subscribe({
      next: (res) => {
        this.loading.set(false);
        const assistantMsg: AiMessage = {
          role: 'assistant',
          content: res.reply,
          createdAt: res.timestamp
        };
        this.messages.update(msgs => [...msgs, assistantMsg]);
        this.shouldScroll = true;

        // If a password was generated, show it
        if (res.generatedPassword) {
          const pwMsg: AiMessage = {
            role: 'assistant',
            content: `Generated password: \`${res.generatedPassword}\`\n\nClick to copy it to your clipboard.`,
            createdAt: new Date().toISOString()
          };
          this.messages.update(msgs => [...msgs, pwMsg]);
        }
      },
      error: () => {
        this.loading.set(false);
        const errorMsg: AiMessage = {
          role: 'assistant',
          content: 'I apologize, but I encountered an error. Please try again.',
          createdAt: new Date().toISOString()
        };
        this.messages.update(msgs => [...msgs, errorMsg]);
        this.shouldScroll = true;
      }
    });
  }

  sendSuggestion(suggestion: string): void {
    this.inputMessage = suggestion.replace(/^[^\s]+\s/, '');
    this.sendMessage();
  }

  onEnterKey(event: KeyboardEvent): void {
    if (!event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  clearChat(): void {
    this.messages.set([]);
    this.http.delete('/api/ai/chat/history').subscribe({ error: () => {} });
  }

  formatMessage(content: string): string {
    return content
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/`(.*?)`/g, '<code>$1</code>')
      .replace(/\n/g, '<br>');
  }

  formatTime(dateStr: string): string {
    return new Date(dateStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  private scrollToBottom(): void {
    try {
      const el = this.messagesContainer.nativeElement;
      el.scrollTop = el.scrollHeight;
    } catch {}
  }
}
