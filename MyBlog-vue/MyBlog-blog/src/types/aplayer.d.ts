declare module 'aplayer' {
  export default class APlayer {
    constructor(options: Record<string, any>)
    destroy: () => void
    play: () => Promise<void> | void
  }
}
