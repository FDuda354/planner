export function matchesSearch(query: string, entry: {note: string; amount: number}, itemName: string | undefined): boolean {
  const normalizedQuery = normalizeForSearch(query.trim());
  if (normalizedQuery === '') {
    return true;
  }
  const amount = entry.amount.toFixed(2);
  return normalizeForSearch(`${entry.note} ${itemName ?? ''} ${amount} ${amount.replace('.', ',')}`).includes(normalizedQuery);
}

export function normalizeForSearch(text: string): string {
  return text.toLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu, '').replaceAll('ł', 'l');
}
