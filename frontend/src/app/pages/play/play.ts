import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { randomCelebrationGif, randomMotivationalMessage } from '../../core/celebration';
import { AnswerResponse, GameResponse, QuestionResponse } from '../../core/models';

const TICK_MILLIS = 200;
const FINISHED_ACTIONS = 3;

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
          {{ finished() ? (current.status === 'DEFEATED' ? 'Derrota' : 'Partida concluida') : secondsLeft() + 's para responder' }}
        </p>
      </section>

      @if (finished()) {
        <section class="panel center" [class.defeat-panel]="current.status === 'DEFEATED'">
          @if (current.status === 'DEFEATED') {
            <svg class="monster" viewBox="0 0 200 200" role="img" aria-label="Monstro que venceu a partida">
              <path d="M52 62 L34 16 L78 46 Z" fill="#7f1d1d"/>
              <path d="M148 62 L166 16 L122 46 Z" fill="#7f1d1d"/>
              <path d="M30 96 L12 88 L30 78 Z" fill="#7f1d1d"/>
              <path d="M170 96 L188 88 L170 78 Z" fill="#7f1d1d"/>
              <path d="M100 34 C142 34 168 64 168 104 C168 148 138 178 100 178 C62 178 32 148 32 104 C32 64 58 34 100 34 Z"
                    fill="#1f2937"/>
              <path d="M58 90 L94 104 L58 118 Z" fill="#ef4444"/>
              <path d="M142 90 L106 104 L142 118 Z" fill="#ef4444"/>
              <path d="M64 134 Q100 162 136 134 Q100 150 64 134 Z" fill="#7f1d1d"/>
              <path d="M74 138 L80 154 L86 138 Z" fill="#f8fafc"/>
              <path d="M96 142 L102 160 L108 142 Z" fill="#f8fafc"/>
              <path d="M118 138 L124 154 L130 138 Z" fill="#f8fafc"/>
            </svg>
            <h1 class="defeat">Voce foi derrotado!</h1>
            <p class="muted">
              No modo Genocida uma resposta errada encerra a partida. O monstro agradece.
            </p>
          } @else {
            <h1>Partida concluida</h1>
          }
          <p class="big">{{ current.score }} pontos</p>
          <p class="muted">
            {{ current.correctAnswers }} acertos &middot; {{ current.wrongAnswers }} erros
          </p>
          <div class="finished-actions">
            <button
              class="primary"
              [class.selected]="finishedAction() === 0"
              (mouseenter)="finishedAction.set(0)"
              (click)="newGame()"
              [disabled]="loading()"
            >
              {{ loading() ? 'Criando...' : 'Jogar novamente' }}
            </button>
            <a
              class="secondary"
              [class.selected]="finishedAction() === 1"
              (mouseenter)="finishedAction.set(1)"
              routerLink="/ranking"
              >Ver ranking</a
            >
            <a
              class="secondary"
              [class.selected]="finishedAction() === 2"
              (mouseenter)="finishedAction.set(2)"
              routerLink="/"
              >Voltar ao inicio</a
            >
          </div>
          <p class="muted center hint">
            <kbd>&larr;</kbd> <kbd>&rarr;</kbd> para escolher e <kbd>Enter</kbd> para confirmar
          </p>
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

    @if (recordBroken()) {
      <div class="celebration-backdrop" (click)="closeCelebration()">
        <div class="celebration" (click)="$event.stopPropagation()">
          <p class="pill ok">Novo recorde</p>
          <h1>Parabens!</h1>
          <p class="big">Voce e o TOP 1 do ranking Genocida!</p>
          <img class="celebration-gif" [src]="celebrationGif()" alt="Comemoracao" />
          <p class="celebration-message">"{{ celebrationMessage() }}"</p>
          <div class="finished-actions">
            <button class="primary" (click)="closeCelebration()">Continuar</button>
            <a class="secondary" routerLink="/ranking" (click)="closeCelebration()">Ver ranking</a>
          </div>
          <p class="muted center hint">
            Pressione <kbd>Enter</kbd> ou <kbd>Esc</kbd> para fechar
          </p>
        </div>
      </div>
    }
  `
})
export class PlayPage implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly gameId = signal('');
  private recordChecked = false;

  private timer?: ReturnType<typeof setInterval>;

  readonly game = signal<GameResponse | null>(null);
  readonly question = signal<QuestionResponse | null>(null);
  readonly feedback = signal<AnswerResponse | null>(null);
  readonly selectedIndex = signal(0);
  readonly finishedAction = signal(0);
  readonly secondsLeft = signal(0);
  readonly timedOut = signal(false);
  readonly finished = signal(false);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly recordBroken = signal(false);
  readonly celebrationMessage = signal('');
  readonly celebrationGif = signal('');

  readonly selectedOption = computed(() => {
    const options = this.question()?.options ?? [];
    return options[this.selectedIndex()] ?? null;
  });

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      this.gameId.set(params.get('id') ?? '');
      this.reset();
      this.loadGame();
    });
  }

  private loadGame(): void {
    this.api.game(this.gameId()).subscribe({
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

  private reset(): void {
    this.stopTimer();
    this.game.set(null);
    this.question.set(null);
    this.feedback.set(null);
    this.selectedIndex.set(0);
    this.finishedAction.set(0);
    this.secondsLeft.set(0);
    this.timedOut.set(false);
    this.finished.set(false);
    this.loading.set(false);
    this.error.set(null);
    this.recordBroken.set(false);
    this.recordChecked = false;
  }

  /** Cria/retoma uma partida no mesmo modo e navega para ela. */
  newGame(): void {
    this.loading.set(true);
    this.api.startGame(this.game()?.mode).subscribe({
      next: (game) => this.router.navigate(['/play', game.id]),
      error: (response: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(response.error?.message ?? 'Nao foi possivel iniciar uma nova partida.');
      }
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
    if (this.recordBroken()) {
      if (event.key === 'Enter' || event.key === 'Escape') {
        event.preventDefault();
        this.closeCelebration();
      }
      return;
    }
    if (this.finished()) {
      if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
        event.preventDefault();
        this.moveFinished(event.key === 'ArrowRight' ? 1 : -1);
        return;
      }
      if (event.key === 'Enter') {
        event.preventDefault();
        this.activateFinished();
      }
      return;
    }
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

  /** Move a selecao entre as acoes da tela de fim (Jogar novamente / Ranking / Inicio). */
  private moveFinished(delta: number): void {
    this.finishedAction.set((this.finishedAction() + delta + FINISHED_ACTIONS) % FINISHED_ACTIONS);
  }

  /** Executa a acao selecionada na tela de fim (Enter). */
  private activateFinished(): void {
    if (this.loading()) {
      return;
    }
    switch (this.finishedAction()) {
      case 0:
        this.newGame();
        return;
      case 1:
        this.router.navigateByUrl('/ranking');
        return;
      default:
        this.router.navigateByUrl('/');
    }
  }

  choose(optionId: string): void {
    this.loading.set(true);
    this.timedOut.set(false);
    this.api.answer(this.gameId(), optionId).subscribe({
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
      this.checkGenocidaRecord();
      return;
    }
    if (result?.nextQuestion) {
      this.startQuestion(result.nextQuestion);
      return;
    }
    this.loadQuestion();
  }

  closeCelebration(): void {
    this.recordBroken.set(false);
  }

  /** Se a partida genocida recem-terminada virou a #1 do ranking, dispara a comemoracao. */
  private checkGenocidaRecord(): void {
    if (this.recordChecked || this.game()?.mode !== 'GENOCIDA') {
      return;
    }
    this.recordChecked = true;
    const me = this.auth.user();
    if (!me) {
      return;
    }
    this.api.game(this.gameId()).subscribe({
      next: (game) => {
        if (game.completedAt === null) {
          return;
        }
        this.api.ranking(0, 1, 'GENOCIDA').subscribe({
          next: (ranking) => {
            const top = ranking.entries[0];
            const sameGame =
              top !== undefined &&
              game.completedAt !== null &&
              new Date(top.achievedAt).getTime() === new Date(game.completedAt).getTime();
            if (top !== undefined && top.userId === me.id && top.score === game.score && sameGame) {
              this.celebrationMessage.set(randomMotivationalMessage());
              this.celebrationGif.set(randomCelebrationGif());
              this.recordBroken.set(true);
            }
          }
        });
      }
    });
  }

  abandon(): void {
    this.loading.set(true);
    this.api.abandon(this.gameId()).subscribe({
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
    this.api.currentQuestion(this.gameId()).subscribe({
      next: (question) => this.startQuestion(question),
      error: () => {
        this.finished.set(true);
        this.refreshGame();
        this.checkGenocidaRecord();
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

    this.api.currentQuestion(this.gameId()).subscribe({
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
        this.checkGenocidaRecord();
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
              status: result.status
            }
          : game
      );
      return;
    }

    this.api.game(this.gameId()).subscribe({ next: (game) => this.game.set(game) });
  }
}
