import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { LoginPage } from './pages/login/login';
import { HomePage } from './pages/home/home';
import { PlayPage } from './pages/play/play';
import { RankingPage } from './pages/ranking/ranking';

export const routes: Routes = [
  { path: 'login', component: LoginPage },
  { path: '', component: HomePage, canActivate: [authGuard] },
  { path: 'play/:id', component: PlayPage, canActivate: [authGuard] },
  { path: 'ranking', component: RankingPage, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
