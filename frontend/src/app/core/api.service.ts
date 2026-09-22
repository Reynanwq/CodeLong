import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AnswerResponse,
  AuthResponse,
  GameResponse,
  PageResponse,
  QuestionResponse,
  RankingEntry,
  RankingResponse,
  ThemeQuestionsResponse,
  ThemesResponse,
  User
} from './models';

const API_BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  register(username: string, email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_BASE}/auth/register`, { username, email, password });
  }

  login(identifier: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_BASE}/auth/login`, { identifier, password });
  }

  me(): Observable<User> {
    return this.http.get<User>(`${API_BASE}/users/me`);
  }

  startGame(mode?: string, category?: string, questionId?: string): Observable<GameResponse> {
    let params = new HttpParams();
    if (mode) {
      params = params.set('mode', mode);
    }
    if (category) {
      params = params.set('category', category);
    }
    if (questionId) {
      params = params.set('questionId', questionId);
    }
    const query = params.toString();
    const url = query ? `${API_BASE}/games?${query}` : `${API_BASE}/games`;
    return this.http.post<GameResponse>(url, null);
  }

  inProgressGame(): Observable<GameResponse | null> {
    return this.http.get<GameResponse | null>(`${API_BASE}/games/in-progress`);
  }

  game(id: string): Observable<GameResponse> {
    return this.http.get<GameResponse>(`${API_BASE}/games/${id}`);
  }

  history(page = 0, size = 10, status?: string): Observable<PageResponse<GameResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<PageResponse<GameResponse>>(`${API_BASE}/games`, { params });
  }

  currentQuestion(gameId: string): Observable<QuestionResponse> {
    return this.http.get<QuestionResponse>(`${API_BASE}/games/${gameId}/current-question`);
  }

  answer(gameId: string, optionId: string): Observable<AnswerResponse> {
    return this.http.post<AnswerResponse>(`${API_BASE}/games/${gameId}/answers`, { optionId });
  }

  abandon(gameId: string): Observable<GameResponse> {
    return this.http.post<GameResponse>(`${API_BASE}/games/${gameId}/abandon`, null);
  }

  ranking(page = 0, size = 10, mode?: string, theme?: string): Observable<RankingResponse> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (mode) {
      params = params.set('mode', mode);
    }
    if (theme) {
      params = params.set('theme', theme);
    }
    return this.http.get<RankingResponse>(`${API_BASE}/rankings`, { params });
  }

  learningThemes(): Observable<ThemesResponse> {
    return this.http.get<ThemesResponse>(`${API_BASE}/learning/themes`);
  }

  learningThemeQuestions(category: string): Observable<ThemeQuestionsResponse> {
    return this.http.get<ThemeQuestionsResponse>(`${API_BASE}/learning/themes/${category}/questions`);
  }

  myRanking(mode?: string): Observable<RankingEntry | null> {
    const params = mode ? new HttpParams().set('mode', mode) : undefined;
    return this.http.get<RankingEntry | null>(`${API_BASE}/rankings/me`, { params });
  }
}
