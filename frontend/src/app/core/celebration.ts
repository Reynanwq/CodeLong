import { MOTIVATIONAL_MESSAGES } from './messages';

/** GIFs locais (baixados do Tenor) servidos de /gifs. */
export const CELEBRATION_GIFS: string[] = [
  '/gifs/anime-confetti.gif',
  '/gifs/anime-happy.gif',
  '/gifs/celebration.gif',
  '/gifs/code.gif',
  '/gifs/code-encoding.gif',
  '/gifs/coding.gif',
  '/gifs/coding-anime.gif',
  '/gifs/coding-code.gif',
  '/gifs/coding-developer.gif',
  '/gifs/coding-impact.gif',
  '/gifs/coding-programming.gif',
  '/gifs/coding-scaler.gif',
  '/gifs/coding-screen.gif',
  '/gifs/coding-typing.gif',
  '/gifs/f1-donuts.gif',
  '/gifs/f1-ferrari.gif',
  '/gifs/f1-formula-1.gif',
  '/gifs/f1-formule.gif',
  '/gifs/f1-kimi.gif',
  '/gifs/f1-leclerc.gif',
  '/gifs/f1-piastri.gif',
  '/gifs/f1-race.gif',
  '/gifs/f1-racing.gif',
  '/gifs/f1-russell.gif',
  '/gifs/f1-speed.gif',
  '/gifs/f1-toto-wolff.gif',
  '/gifs/f1-verstappen.gif',
  '/gifs/f1-winner.gif',
  '/gifs/fireworks.gif',
  '/gifs/gaming.gif',
  '/gifs/gaming-adam.gif',
  '/gifs/gaming-computer.gif',
  '/gifs/gaming-fortnite.gif',
  '/gifs/gaming-lean.gif',
  '/gifs/gaming-night.gif',
  '/gifs/gaming-old.gif',
  '/gifs/gaming-play.gif',
  '/gifs/gaming-racing.gif',
  '/gifs/gaming-raid.gif',
  '/gifs/gaming-time.gif',
  '/gifs/gaming-timmy.gif',
  '/gifs/gaming-tryhard.gif',
  '/gifs/hacker-font.gif',
  '/gifs/hacker-pc.gif',
  '/gifs/hacking-computer.gif',
  '/gifs/lets-go.gif',
  '/gifs/party-time.gif',
  '/gifs/python.gif',
  '/gifs/stackoverflow.gif',
  '/gifs/victory-winning.gif'
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
