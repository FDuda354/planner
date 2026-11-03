export function matchesSearch(query: string, entry: {note: string; amount: number}, itemName: string | undefined): boolean {
  const normalizedQuery = query.trim().toLowerCase();
  if (normalizedQuery === '') {
    return true;
  }
  const amount = entry.amount.toFixed(2);
  return `${entry.note} ${itemName ?? ''} ${amount} ${amount.replace('.', ',')}`.toLowerCase().includes(normalizedQuery);
}

export function normalizeForSearch(text: string): string {
  return text.toLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu, '').replace(/ł/g, 'l');
}
