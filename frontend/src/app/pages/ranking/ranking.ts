import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { RankingEntry } from '../../core/models';

@Component({
  selector: 'app-ranking',
  imports: [RouterLink, DatePipe],
  template: `
    <section class="panel">
      <div class="row space-between">
        <h1>Ranking</h1>
        <a class="link" routerLink="/">Voltar</a>
      </div>

      @if (entries().length === 0) {
        <p class="muted">Ninguem concluiu uma partida ainda.</p>
      } @else {
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>Jogador</th>
              <th>Pontos</th>
              <th>Acertos</th>
              <th>Perguntas</th>
              <th>Tempo</th>
              <th>Quando</th>
            </tr>
          </thead>
          <tbody>
            @for (entry of entries(); track entry.userId) {
              <tr>
                <td>{{ entry.position }}</td>
                <td>{{ entry.username }}</td>
                <td><strong>{{ entry.score }}</strong></td>
                <td>{{ entry.correctAnswers }}</td>
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

  ngOnInit(): void {
    this.load(0);
  }

  goTo(page: number): void {
    this.load(page);
  }

  duration(entry: RankingEntry): string {
    const seconds = Math.round(entry.totalTimeMillis / 1000);
    return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
  }

  private load(page: number): void {
    this.api.ranking(page, 10).subscribe({
      next: (response) => {
        this.entries.set(response.entries);
        this.page.set(response.page);
        this.totalPages.set(response.totalPages);
      }
    });
  }
}
