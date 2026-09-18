import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { ApiService } from './api.service';
import { AuthResponse, User } from './models';

const TOKEN_KEY = 'codelong.token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly currentUser = signal<User | null>(null);

  readonly user = this.currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this.currentUser() !== null);

  constructor() {
    if (this.token) {
      this.loadCurrentUser();
    }
  }

  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  login(identifier: string, password: string): Observable<AuthResponse> {
    return this.api.login(identifier, password).pipe(tap((response) => this.apply(response)));
  }

  register(username: string, email: string, password: string): Observable<AuthResponse> {
    return this.api.register(username, email, password).pipe(tap((response) => this.apply(response)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.currentUser.set(null);
    this.router.navigateByUrl('/login');
  }

  private apply(response: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, response.token);
    this.currentUser.set(response.user);
  }

  private loadCurrentUser(): void {
    this.api.me().subscribe({
      next: (user) => this.currentUser.set(user),
      error: () => this.logout()
    });
  }
}
