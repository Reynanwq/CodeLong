import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { RankingEntry, Theme } from '../../core/models';

type RankingMode = 'CLASSIC' | 'GENOCIDA' | 'APRENDIZADO';

@Component({
  selector: 'app-ranking',
  imports: [RouterLink, DatePipe],
  template: `
    <section class="panel">
      <div class="row space-between">
        <h1>Ranking</h1>
        <a class="link" routerLink="/">Voltar</a>
      </div>

      <div class="tabs">
        <button
          type="button"
          class="tab"
          [class.active]="mode() === 'CLASSIC'"
          (click)="setMode('CLASSIC')"
        >
          Classico
        </button>
        <button
          type="button"
          class="tab danger"
          [class.active]="mode() === 'GENOCIDA'"
          (click)="setMode('GENOCIDA')"
        >
          Genocida
        </button>
        <button
          type="button"
          class="tab"
          [class.active]="mode() === 'APRENDIZADO'"
          (click)="setMode('APRENDIZADO')"
        >
          Aprendizado
        </button>
      </div>

      @if (mode() === 'APRENDIZADO') {
        <div class="row">
          <label class="muted" for="theme">Tema:</label>
          <select
            id="theme"
            [value]="theme() ?? ''"
            (change)="setTheme($any($event.target).value)"
          >
            @for (item of themes(); track item.category) {
              <option [value]="item.category">{{ item.category }} ({{ item.totalQuestions }})</option>
            }
          </select>
        </div>
      }

      @if (entries().length === 0) {
        <p class="muted">Ninguem concluiu uma partida no modo {{ label() }} ainda.</p>
      } @else {
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>Jogador</th>
              <th>Pontos</th>
              <th>Acertos</th>
              <th>Erros</th>
              <th>Perguntas</th>
              <th>Tempo</th>
              <th>Quando</th>
            </tr>
          </thead>
          <tbody>
            @for (entry of entries(); track $index) {
              <tr>
                <td>{{ entry.position }}</td>
                <td>{{ entry.username }}</td>
                <td><strong>{{ entry.score }}</strong></td>
                <td>{{ entry.correctAnswers }}</td>
                <td class="errors">{{ entry.wrongAnswers }}</td>
                <td>{{ entry.answeredQuestions }}</td>
                <td>{{ duration(entry) }}</td>
                <td>{{ entry.achievedAt | date: 'dd/MM HH:mm' }}</td>
              </tr>
            }
          </tbody>
        </table>

        <div class="pager">
          <button class="ghost" (click)="goTo(page() - 1)" [disabled]="page() === 0">Anterior</button>
          <span class="muted">{{ page() + 1 }} / {{ totalPages() }}</span>
          <button class="ghost" (click)="goTo(page() + 1)" [disabled]="page() + 1 >= totalPages()">
            Proxima
          </button>
        </div>
      }
    </section>
  `
})
export class RankingPage implements OnInit {
  private readonly api = inject(ApiService);

  readonly entries = signal<RankingEntry[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly mode = signal<RankingMode>('CLASSIC');
  readonly themes = signal<Theme[]>([]);
  readonly theme = signal<string | null>(null);

  ngOnInit(): void {
    this.api.learningThemes().subscribe({
      next: (response) => {
        this.themes.set(response.themes);
        if (this.mode() === 'APRENDIZADO' && !this.theme() && response.themes.length > 0) {
          this.theme.set(response.themes[0].category);
          this.load(0);
        }
      }
    });
    this.load(0);
  }

  setMode(mode: RankingMode): void {
    if (this.mode() === mode) {
      return;
    }
    this.mode.set(mode);
    if (mode === 'APRENDIZADO' && !this.theme() && this.themes().length > 0) {
      this.theme.set(this.themes()[0].category);
    }
    this.load(0);
  }

  setTheme(category: string): void {
    this.theme.set(category);
    this.load(0);
  }

  label(): string {
    if (this.mode() === 'GENOCIDA') {
      return 'Genocida';
    }
    return this.mode() === 'APRENDIZADO' ? 'Aprendizado' : 'Classico';
  }

  goTo(page: number): void {
    this.load(page);
  }

  duration(entry: RankingEntry): string {
    const seconds = Math.round(entry.totalTimeMillis / 1000);
    return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
  }

  private load(page: number): void {
    const theme = this.mode() === 'APRENDIZADO' ? this.theme() ?? undefined : undefined;
    if (this.mode() === 'APRENDIZADO' && !theme) {
      this.entries.set([]);
      this.page.set(0);
      this.totalPages.set(0);
      return;
    }
    this.api.ranking(page, 10, this.mode(), theme).subscribe({
      next: (response) => {
        this.entries.set(response.entries);
        this.page.set(response.page);
        this.totalPages.set(response.totalPages);
      }
    });
  }
}
