export interface ChatPostSharePayload {
  postId: number;
  title: string;
  coverUrl?: string | null;
}

export interface ChatActivitySharePayload {
  activityId: number;
  title: string;
  coverUrl?: string | null;
  startTime?: string | null;
  location?: string | null;
}

export function parseChatPostSharePayload(content: string): ChatPostSharePayload | null {
  try {
    const parsed = JSON.parse(content) as Partial<ChatPostSharePayload>;
    if (typeof parsed.postId !== 'number' || typeof parsed.title !== 'string') {
      return null;
    }
    return {
      postId: parsed.postId,
      title: parsed.title.trim() || '笔记',
      coverUrl: parsed.coverUrl ?? null,
    };
  } catch {
    return null;
  }
}

export function parseChatActivitySharePayload(
  content: string,
): ChatActivitySharePayload | null {
  try {
    const parsed = JSON.parse(content) as Partial<ChatActivitySharePayload>;
    if (typeof parsed.activityId !== 'number' || typeof parsed.title !== 'string') {
      return null;
    }
    return {
      activityId: parsed.activityId,
      title: parsed.title.trim() || '活动',
      coverUrl: parsed.coverUrl ?? null,
      startTime: parsed.startTime ?? null,
      location: parsed.location ?? null,
    };
  } catch {
    return null;
  }
}

export function formatChatPostSharePreview(payload: ChatPostSharePayload): string {
  const title = payload.title.trim().slice(0, 30) || '笔记';
  return `[笔记] ${title}`;
}

export function formatChatActivitySharePreview(payload: ChatActivitySharePayload): string {
  const title = payload.title.trim().slice(0, 30) || '活动';
  return `[活动] ${title}`;
}
