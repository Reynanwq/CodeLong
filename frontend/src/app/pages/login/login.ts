import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <div class="auth-card">
      <h1>CodeLong</h1>
      <p class="muted">Perguntas de programacao e engenharia de software</p>

      <div class="tabs">
        <button type="button" [class.active]="mode() === 'login'" (click)="switchMode('login')">Entrar</button>
        <button type="button" [class.active]="mode() === 'register'" (click)="switchMode('register')">
          Criar conta
        </button>
      </div>

      @if (error()) {
        <div class="alert">{{ error() }}</div>
      }

      <form (ngSubmit)="submit()">
        @if (mode() === 'register') {
          <label>
            Usuario
            <input name="username" [(ngModel)]="username" autocomplete="username" required />
          </label>
          <label>
            E-mail
            <input name="email" type="email" [(ngModel)]="email" autocomplete="email" required />
          </label>
        } @else {
          <label>
            Usuario ou e-mail
            <input name="identifier" [(ngModel)]="identifier" autocomplete="username" required />
          </label>
        }

        <label>
          Senha
          <input name="password" type="password" [(ngModel)]="password" autocomplete="current-password" required />
        </label>

        <button type="submit" class="primary" [disabled]="loading()">
          {{ loading() ? 'Aguarde...' : (mode() === 'login' ? 'Entrar' : 'Criar conta') }}
        </button>
      </form>
    </div>
  `
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly mode = signal<'login' | 'register'>('login');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  username = '';
  email = '';
  identifier = '';
  password = '';

  switchMode(mode: 'login' | 'register'): void {
    this.mode.set(mode);
    this.error.set(null);
  }

  submit(): void {
    this.loading.set(true);
    this.error.set(null);

    const request =
      this.mode() === 'login'
        ? this.auth.login(this.identifier, this.password)
        : this.auth.register(this.username, this.email, this.password);

    request.subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl('/');
      },
      error: (response: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(this.messageOf(response));
      }
    });
  }

  private messageOf(response: HttpErrorResponse): string {
    if (response.status === 401) {
      return 'Credenciais invalidas.';
    }
    if (response.status === 409) {
      return response.error?.code === 'USERNAME_ALREADY_EXISTS'
        ? 'Este usuario ja existe.'
        : 'Este e-mail ja esta cadastrado.';
    }
    return response.error?.message ?? 'Nao foi possivel autenticar. Verifique os dados.';
  }
}
