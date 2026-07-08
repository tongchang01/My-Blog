export function getDaysTillNow(from: string) {
  const today = new Date()
  const fromDate = new Date(from)
  const timeDiff = today.getTime() - fromDate.getTime()
  return Math.floor(timeDiff / (1000 * 3600 * 24))
}

export function shuffleArray<T = any>(array: T[]): T[] {
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[array[i], array[j]] = [array[j], array[i]]
  }
  return array
}

export function throttle(func: () => void, timeFrame: number) {
  let time = Number(new Date())
  return function () {
    if (time + timeFrame - Date.now() < 0) {
      func()
      time = Date.now()
    }
  }
}

export function paginator<T>(data: T[], page: number, pageSize: number) {
  const skip = pageSize * (page - 1)
  const endIndex = skip > data.length - 1 ? undefined : pageSize * page
  return data.slice(skip, endIndex)
}
