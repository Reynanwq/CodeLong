import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { LearningQuestion, Theme } from '../../core/models';

@Component({
  selector: 'app-learning',
  imports: [RouterLink],
  template: `
    <section class="panel">
      <div class="row space-between">
        <h1>Aprendizado</h1>
        <a class="link" routerLink="/">Voltar</a>
      </div>
      <p class="muted">
        Escolha um tema para estudar. Voce pode responder uma pergunta especifica ou jogar o tema
        inteiro (morte subita, como no Genocida).
      </p>

      @if (error()) {
        <div class="alert">{{ error() }}</div>
      }

      @if (themes().length === 0) {
        <p class="muted">Nenhum tema disponivel.</p>
      } @else {
        <div class="tabs">
          @for (theme of themes(); track theme.category) {
            <button
              type="button"
              class="tab"
              [class.active]="selected() === theme.category"
              (click)="select(theme.category)"
            >
              {{ theme.category }} ({{ theme.totalQuestions }})
            </button>
          }
        </div>
      }

      @if (selected(); as category) {
        <div class="row space-between">
          <h2>{{ category }}</h2>
          <button class="primary" (click)="startTheme(category)" [disabled]="loading()">
            {{ loading() ? 'Criando...' : 'Iniciar tema' }}
          </button>
        </div>

        @if (questions().length === 0) {
          <p class="muted">Carregando perguntas...</p>
        } @else {
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>Pergunta</th>
                <th>Dificuldade</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (question of questions(); track question.id; let i = $index) {
                <tr>
                  <td>{{ i + 1 }}</td>
                  <td class="statement-cell">{{ question.statement }}</td>
                  <td><span class="pill">{{ question.difficulty }}</span></td>
                  <td>
                    <button class="ghost" (click)="answerOne(question)" [disabled]="loading()">
                      Responder
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      }
    </section>
  `
})
export class LearningPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  readonly themes = signal<Theme[]>([]);
  readonly selected = signal<string | null>(null);
  readonly questions = signal<LearningQuestion[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.api.learningThemes().subscribe({
      next: (response) => {
        this.themes.set(response.themes);
        if (response.themes.length > 0) {
          this.select(response.themes[0].category);
        }
      },
      error: () => this.error.set('Nao foi possivel carregar os temas.')
    });
  }

  select(category: string): void {
    if (this.selected() === category) {
      return;
    }
    this.selected.set(category);
    this.questions.set([]);
    this.error.set(null);
    this.api.learningThemeQuestions(category).subscribe({
      next: (response) => this.questions.set(response.questions),
      error: () => this.error.set('Nao foi possivel carregar as perguntas do tema.')
    });
  }

  startTheme(category: string): void {
    this.start(category, undefined);
  }

  answerOne(question: LearningQuestion): void {
    this.start(undefined, question.id);
  }

  private start(category?: string, questionId?: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.startGame('APRENDIZADO', category, questionId).subscribe({
      next: (game) => this.router.navigate(['/play', game.id]),
      error: (response: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(response.error?.message ?? 'Nao foi possivel iniciar a partida.');
      }
    });
  }
}
