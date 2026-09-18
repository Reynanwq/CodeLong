export interface User {
  id: string;
  username: string;
  email: string;
  role: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}

export interface GameResponse {
  id: string;
  status: string;
  currentQuestionIndex: number;
  currentQuestionDeadline: string;
  totalQuestions: number;
  remainingQuestions: number;
  score: number;
  correctAnswers: number;
  wrongAnswers: number;
  startedAt: string;
  completedAt: string | null;
}

export interface OptionResponse {
  id: string;
  text: string;
}

export interface QuestionResponse {
  id: string;
  statement: string;
  options: OptionResponse[];
  category: string;
  difficulty: string;
  deadline: string;
  timeLimitSeconds: number;
}

export interface AnswerResponse {
  correct: boolean;
  timedOut: boolean;
  chosenOption: string | null;
  correctOption: string;
  explanation: string;
  earnedPoints: number;
  currentScore: number;
  correctAnswers: number;
  wrongAnswers: number;
  gameCompleted: boolean;
  questionIndex: number;
  totalQuestions: number;
  nextQuestion: QuestionResponse | null;
}

export interface PageResponse<T> {
  items: T[];
  totalElements: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface RankingEntry {
  position: number | null;
  userId: string;
  username: string;
  score: number;
  correctAnswers: number;
  answeredQuestions: number;
  totalTimeMillis: number;
  achievedAt: string;
}

export interface RankingResponse {
  entries: RankingEntry[];
  totalElements: number;
  page: number;
  size: number;
  totalPages: number;
}
