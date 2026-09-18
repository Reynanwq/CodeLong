import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { AnswerResponse, GameResponse, QuestionResponse } from '../../core/models';

const TICK_MILLIS = 200;

@Component({
  selector: 'app-play',
  imports: [RouterLink],
  host: { '(document:keydown)': 'onKeydown($event)' },
  template: `
    @if (error()) {
      <section class="panel">
        <div class="alert">{{ error() }}</div>
        <a class="link" routerLink="/">Voltar</a>
      </section>
    }

    @if (game(); as current) {
      <section class="panel">
        <div class="row space-between">
          <div>
            <span class="pill">{{ current.currentQuestionIndex + 1 }} / {{ current.totalQuestions }}</span>
            <span class="pill ok">Pontos: {{ current.score }}</span>
            @if (current.mode === 'GENOCIDA') {
              <span class="pill danger">GENOCIDA</span>
            }
          </div>
          <button class="ghost" (click)="abandon()" [disabled]="loading()">Abandonar</button>
        </div>

        <div class="bar timer" [class.danger]="secondsLeft() <= 5">
          <span [style.width.%]="timerPercent()"></span>
        </div>
        <p class="muted center">
          {{ finished() ? 'Partida concluida' : secondsLeft() + 's para responder' }}
        </p>
      </section>

      @if (finished()) {
        <section class="panel center">
          <h1>Partida concluida</h1>
          <p class="big">{{ current.score }} pontos</p>
          <p class="muted">
            {{ current.correctAnswers }} acertos &middot; {{ current.wrongAnswers }} erros
          </p>
          <a class="primary" routerLink="/ranking">Ver ranking</a>
          <a class="link" routerLink="/">Voltar ao inicio</a>
        </section>
      } @else if (question(); as currentQuestion) {
        <section class="panel">
          @if (timedOut()) {
            <div class="alert">Tempo esgotado! Voce perdeu esta pergunta.</div>
          }

          <p class="pill">{{ currentQuestion.category }} &middot; {{ currentQuestion.difficulty }}</p>
          <h2>{{ currentQuestion.statement }}</h2>

          <div class="options">
            @for (option of currentQuestion.options; track option.id; let i = $index) {
              <button
                type="button"
                class="option"
                [class.selected]="selectedIndex() === i"
                [class.correct]="feedback()?.correctOption === option.id"
                [class.wrong]="feedback()?.chosenOption === option.id && !feedback()?.correct"
                [disabled]="feedback() !== null || loading()"
                (mouseenter)="selectedIndex.set(i)"
                (click)="choose(option.id)"
              >
                <strong>{{ letter(i) }})</strong> {{ option.text }}
              </button>
            }
          </div>

          @if (feedback() === null) {
            <p class="muted center hint">Use <kbd>&uarr;</kbd> <kbd>&darr;</kbd> para navegar e <kbd>Enter</kbd> para responder</p>
          }

          @if (feedback(); as result) {
            <div class="feedback" [class.ok]="result.correct" [class.bad]="!result.correct">
              <strong>
                @if (result.timedOut) {
                  Tempo esgotado
                } @else if (result.correct) {
                  Correto! +{{ result.earnedPoints }} pontos
                } @else {
                  Incorreto
                }
              </strong>
              <p>{{ result.explanation }}</p>
            </div>
            <button class="primary" (click)="next()" [disabled]="loading()">Proxima pergunta</button>
            <p class="muted center hint">Pressione <kbd>Enter</kbd> para continuar</p>
          }
        </section>
      }
    } @else if (!error()) {
      <section class="panel center">
        <p class="muted">Carregando partida...</p>
      </section>
    }
  `
})
export class PlayPage implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly gameId = this.route.snapshot.paramMap.get('id') ?? '';
  private timer?: ReturnType<typeof setInterval>;

  readonly game = signal<GameResponse | null>(null);
  readonly question = signal<QuestionResponse | null>(null);
  readonly feedback = signal<AnswerResponse | null>(null);
  readonly selectedIndex = signal(0);
  readonly secondsLeft = signal(0);
  readonly timedOut = signal(false);
  readonly finished = signal(false);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly selectedOption = computed(() => {
    const options = this.question()?.options ?? [];
    return options[this.selectedIndex()] ?? null;
  });

  ngOnInit(): void {
    this.api.game(this.gameId).subscribe({
      next: (game) => {
        this.game.set(game);
        this.finished.set(game.status !== 'IN_PROGRESS');
        if (game.status === 'IN_PROGRESS') {
          this.loadQuestion();
        }
      },
      error: () => this.error.set('Partida nao encontrada.')
    });
  }

  ngOnDestroy(): void {
    this.stopTimer();
  }

  timerPercent(): number {
    const total = this.question()?.timeLimitSeconds ?? 20;
    return total === 0 ? 0 : Math.round((this.secondsLeft() / total) * 100);
  }

  /** Rotulo da alternativa pela POSICAO (as alternativas vem embaralhadas). */
  letter(index: number): string {
    return String.fromCharCode(65 + index);
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      this.move(event.key === 'ArrowDown' ? 1 : -1);
      return;
    }
    if (event.key === 'Enter') {
      event.preventDefault();
      this.confirm();
    }
  }

  choose(optionId: string): void {
    this.loading.set(true);
    this.timedOut.set(false);
    this.api.answer(this.gameId, optionId).subscribe({
      next: (result) => {
        this.loading.set(false);
        this.stopTimer();
        this.feedback.set(result);
        this.refreshGame(result);
      },
      error: (response: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(response.error?.message ?? 'Nao foi possivel registrar a resposta.');
      }
    });
  }

  next(): void {
    const result = this.feedback();
    this.feedback.set(null);
    this.timedOut.set(false);

    if (result?.gameCompleted) {
      this.finished.set(true);
      return;
    }
    if (result?.nextQuestion) {
      this.startQuestion(result.nextQuestion);
      return;
    }
    this.loadQuestion();
  }

  abandon(): void {
    this.loading.set(true);
    this.api.abandon(this.gameId).subscribe({
      next: () => {
        this.stopTimer();
        this.router.navigateByUrl('/');
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Nao foi possivel abandonar a partida.');
      }
    });
  }

  private move(delta: number): void {
    const options = this.question()?.options ?? [];
    if (options.length === 0 || this.feedback() !== null || this.finished() || this.loading()) {
      return;
    }
    this.selectedIndex.set((this.selectedIndex() + delta + options.length) % options.length);
  }

  private confirm(): void {
    if (this.feedback()) {
      this.next();
      return;
    }
    const option = this.selectedOption();
    if (option && !this.loading() && !this.finished()) {
      this.choose(option.id);
    }
  }

  private loadQuestion(): void {
    this.api.currentQuestion(this.gameId).subscribe({
      next: (question) => this.startQuestion(question),
      error: () => {
        this.finished.set(true);
        this.refreshGame();
      }
    });
  }

  private startQuestion(question: QuestionResponse): void {
    this.question.set(question);
    this.selectedIndex.set(0);
    this.startTimer();
  }

  private startTimer(): void {
    this.stopTimer();
    this.tick();
    this.timer = setInterval(() => this.tick(), TICK_MILLIS);
  }

  private stopTimer(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = undefined;
    }
  }

  private tick(): void {
    const question = this.question();
    if (!question) {
      this.secondsLeft.set(0);
      return;
    }

    const remaining = Math.max(0, Math.ceil((new Date(question.deadline).getTime() - Date.now()) / 1000));
    this.secondsLeft.set(remaining);

    if (remaining === 0 && !this.feedback() && !this.loading() && !this.finished()) {
      this.handleTimeout();
    }
  }

  private handleTimeout(): void {
    this.stopTimer();
    this.loading.set(true);
    const previousId = this.question()?.id;

    this.api.currentQuestion(this.gameId).subscribe({
      next: (question) => {
        this.loading.set(false);
        if (question.id !== previousId) {
          this.timedOut.set(true);
          this.startQuestion(question);
          this.refreshGame();
        }
      },
      error: () => {
        this.loading.set(false);
        this.finished.set(true);
        this.refreshGame();
      }
    });
  }

  private refreshGame(result?: AnswerResponse): void {
    if (result) {
      this.game.update((game) =>
        game
          ? {
              ...game,
              score: result.currentScore,
              correctAnswers: result.correctAnswers,
              wrongAnswers: result.wrongAnswers,
              currentQuestionIndex: result.questionIndex + 1,
              status: result.gameCompleted ? 'COMPLETED' : game.status
            }
          : game
      );
      return;
    }

    this.api.game(this.gameId).subscribe({ next: (game) => this.game.set(game) });
  }
}
