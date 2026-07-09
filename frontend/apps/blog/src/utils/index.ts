export function getDaysTillNow(from: string) {
  const today = new Date()
  const fromDate = new Date(from)
  const timeDiff = today.getTime() - fromDate.getTime()
  return Math.floor(timeDiff / (1000 * 3600 * 24))
}
