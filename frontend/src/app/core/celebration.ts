import { MOTIVATIONAL_MESSAGES } from './messages';

/** GIFs locais (baixados do Tenor) servidos de /gifs. */
export const CELEBRATION_GIFS: string[] = [
  '/gifs/anime-happy.gif',
  '/gifs/anime-jump-yay.gif',
  '/gifs/anime-confetti.gif',
  '/gifs/anime-victory.gif',
  '/gifs/anime-excited.gif',
  '/gifs/coding.gif',
  '/gifs/coding-vibes.gif',
  '/gifs/coding-programming.gif',
  '/gifs/coding-code.gif',
  '/gifs/coding-anime.gif'
];

function pick(items: string[]): string {
  return items[Math.floor(Math.random() * items.length)];
}

export function randomCelebrationGif(): string {
  return pick(CELEBRATION_GIFS);
}

export function randomMotivationalMessage(): string {
  return pick(MOTIVATIONAL_MESSAGES);
}
