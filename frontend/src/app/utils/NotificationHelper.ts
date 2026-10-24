import {HttpErrorResponse} from '@angular/common/http';
import {MessageService} from 'primeng/api';
import {ApiError} from '../models/ApiError';

export function showError(title: string, content: string, messageService: MessageService) {
  messageService.add({
    key: 'br',
    severity: 'error',
    summary: title,
    detail: content,
    life: 10000
  });
}

export function showSuccess(title: string, content: string, messageService: MessageService) {
  messageService.add({
    key: 'br',
    severity: 'success',
    summary: title,
    detail: content,
    life: 5000
  });
}

export function showWarn(title: string, content: string, messageService: MessageService) {
  messageService.add({
    key: 'br',
    severity: 'warn',
    summary: title,
    detail: content,
    life: 5000
  });
}

export function showServerError(fallback: string, error: unknown, messageService: MessageService) {
  const message = serverMessage(error);
  if (message) {
    showWarn(fallback, message, messageService);
  } else {
    showError('Błąd serwera', fallback, messageService);
  }
}

export function serverMessage(error: unknown): string | undefined {
  if (!(error instanceof HttpErrorResponse) || error.status < 400 || error.status >= 500) {
    return undefined;
  }

  const msg = (error.error as ApiError | null)?.msg;
  return typeof msg === 'string' && msg.trim() ? msg : undefined;
}
