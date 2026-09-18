import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { GameResponse, RankingEntry } from '../../core/models';

@Component({
  selector: 'app-home',
  imports: [RouterLink, DatePipe],
  template: `
    <section class="panel">
      <div class="row space-between">
        <div>
          <h1>Ola, {{ auth.user()?.username }}</h1>
          <p class="muted">Pronto para o desafio?</p>
        </div>
        <div class="actions">
          @if (inProgress()) {
            <a class="primary" [routerLink]="['/play', inProgress()!.id]">Continuar partida</a>
          } @else {
            <button class="primary" (click)="start('CLASSIC')" [disabled]="loading()">
              {{ loading() ? 'Criando...' : 'Nova partida' }}
            </button>
            <button class="genocide" (click)="start('GENOCIDA')" [disabled]="loading()">
              Modo Genocida
            </button>
          }
        </div>
      </div>

      @if (error()) {
        <div class="alert">{{ error() }}</div>
      }

      @if (inProgress(); as game) {
        <div class="progress-card">
          <div class="row space-between">
            <strong>Partida em andamento</strong>
            <span class="pill">{{ game.currentQuestionIndex }} / {{ game.totalQuestions }}</span>
          </div>
          <p class="muted">
            Modo: <strong>{{ game.mode === 'GENOCIDA' ? 'Genocida' : 'Classico' }}</strong>
          </p>
          <div class="bar">
            <span [style.width.%]="percent(game)"></span>
          </div>
          <p class="muted">
            Pontos: <strong>{{ game.score }}</strong> &middot; acertos: {{ game.correctAnswers }} &middot;
            erros: {{ game.wrongAnswers }}
          </p>
        </div>
      }
    </section>

    <section class="grid">
      <div class="panel">
        <h2>Meu ranking</h2>
        @if (myRanking(); as mine) {
          <p class="big">{{ mine.position }}o lugar</p>
          <p class="muted">{{ mine.score }} pontos &middot; {{ mine.correctAnswers }} acertos</p>
        } @else {
          <p class="muted">Conclua uma partida para entrar no ranking.</p>
        }
        <a class="link" routerLink="/ranking">Ver ranking completo</a>
      </div>

      <div class="panel">
        <h2>Historico</h2>
        @if (history().length === 0) {
          <p class="muted">Nenhuma partida ainda.</p>
        } @else {
          <table>
            <thead>
              <tr>
                <th>Status</th>
                <th>Pontos</th>
                <th>Acertos</th>
                <th>Inicio</th>
              </tr>
            </thead>
            <tbody>
              @for (game of history(); track game.id) {
                <tr>
                  <td><span class="pill" [class.ok]="game.status === 'COMPLETED'">{{ game.status }}</span></td>
                  <td>{{ game.score }}</td>
                  <td>{{ game.correctAnswers }}/{{ game.totalQuestions }}</td>
                  <td>{{ game.startedAt | date: 'dd/MM HH:mm' }}</td>
                </tr>
              }
            </tbody>
          </table>
        }
      </div>
    </section>
  `
})
export class HomePage implements OnInit {
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);

  readonly inProgress = signal<GameResponse | null>(null);
  readonly history = signal<GameResponse[]>([]);
  readonly myRanking = signal<RankingEntry | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  start(mode: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.startGame(mode).subscribe({
      next: () => this.load(),
      error: (response: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(response.error?.message ?? 'Nao foi possivel iniciar a partida.');
      }
    });
  }

  percent(game: GameResponse): number {
    return game.totalQuestions === 0
      ? 0
      : Math.round((game.currentQuestionIndex / game.totalQuestions) * 100);
  }

  private load(): void {
    this.loading.set(false);
    this.api.inProgressGame().subscribe({ next: (game) => this.inProgress.set(game) });
    this.api.history(0, 5).subscribe({ next: (page) => this.history.set(page.items) });
    this.api.myRanking().subscribe({ next: (entry) => this.myRanking.set(entry) });
  }
}
