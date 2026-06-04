export interface ChatProductPayload {
  postId: number;
  title: string;
  price: string;
  coverUrl?: string | null;
  status?: string;
  category?: string;
}

export function parseChatProductPayload(content: string): ChatProductPayload | null {
  try {
    const parsed = JSON.parse(content) as Partial<ChatProductPayload>;
    if (
      typeof parsed.postId !== 'number' ||
      typeof parsed.title !== 'string' ||
      typeof parsed.price !== 'string'
    ) {
      return null;
    }
    return {
      postId: parsed.postId,
      title: parsed.title,
      price: parsed.price,
      coverUrl: parsed.coverUrl ?? null,
      status: parsed.status,
      category: parsed.category,
    };
  } catch {
    return null;
  }
}

export function formatChatProductPreview(payload: ChatProductPayload): string {
  const price = Number.parseFloat(payload.price).toFixed(2);
  const title = payload.title.trim().slice(0, 30) || '商品';
  return `[商品] ${title} ¥${price}`;
}

export function buildChatProductPayload(input: {
  postId: number;
  title: string;
  price: string;
  coverUrl?: string | null;
  status?: string;
  category?: string;
}): ChatProductPayload {
  return {
    postId: input.postId,
    title: input.title.trim() || '商品',
    price: Number.parseFloat(input.price).toFixed(2),
    coverUrl: input.coverUrl ?? null,
    status: input.status,
    category: input.category,
  };
}
