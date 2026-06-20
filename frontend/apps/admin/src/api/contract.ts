export interface ApiResponse<T> {
  code: string;
  msg: string;
  data: T;
}
