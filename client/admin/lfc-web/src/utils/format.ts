export function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatFee(fee: string) {
  const amount = Number(fee)
  if (Number.isNaN(amount) || amount <= 0) {
    return '免费'
  }
  return `¥${amount.toFixed(2)}`
}
