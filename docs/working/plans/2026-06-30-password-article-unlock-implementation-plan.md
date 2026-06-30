# PASSWORD 文章完整解锁实施思路

> 状态：方案已定 / 实现待设计。本文记录 O-001 的实施边界。

## 目标

PASSWORD 文章需要形成完整读者端闭环：

- 未解锁时可以展示锁定元数据和密码输入入口。
- 解锁成功后可以查看正文。
- 解锁成功后可以读取和提交该文章评论。
- 文章访问凭证与后台登录凭证分离，互不复用。

## 已定规则

### 公开文章详情

- `PUBLISHED`：直接返回正文，`locked=false`。
- `PASSWORD` 无有效 token：返回锁定元数据，`locked=true`，`body=null`。
- `PASSWORD` 有有效 token：返回正文，`locked=false`。
- `DRAFT / PRIVATE / SCHEDULED / 已删除 / 未到发布时间`：仍按不可见处理，返回 404。

锁定元数据用于直接访问文章 URL 时渲染锁定页，至少包含标题、摘要、slug、发布时间、封面、分类和标签等公开元信息；不得返回正文。

### 解锁接口

建议接口：

```http
POST /api/public/articles/{id}/unlock
Content-Type: application/json
```

请求体：

```json
{
  "password": "plain text"
}
```

成功响应：

```json
{
  "articleToken": "jwt",
  "expiresIn": 1800
}
```

规则：

- 仅 PASSWORD 且公开可访问的文章允许解锁。
- 密码使用现有 BCrypt hash 校验。
- 解锁失败返回 `401 + 20002`。
- 非 PASSWORD、不可见或已删除文章不签发 token。
- 同 IP + 同 article 限流，例如 5 次 / 10 分钟冷却，超限返回 `429 + 90002`。

### Article Access Token

Article Access Token 是独立 JWT，不是后台登录 access token。

建议 payload：

```json
{
  "typ": "article_access",
  "aid": "articleId",
  "iat": 1719710000,
  "exp": 1719711800,
  "iss": "myblog-v2"
}
```

规则：

- 不包含 `sub`，不能代表后台身份。
- `typ` 必须是 `article_access`。
- `aid` 必须等于当前请求文章 ID。
- 默认有效期 30 分钟。
- 后台 access token 不能用于解锁文章；article token 不能用于后台认证。

### 前台保存方式

- 前台使用 `sessionStorage` 保存 Article Access Token。
- key 按文章 ID 区分，例如 `myblog.article-access.<articleId>`。
- 保存 token 和过期时间。
- 页面刷新后同一 tab 内可继续访问；关闭 tab 后自然失效。
- token 到期、详情或评论接口返回认证失败时，前台清理该文章 token 并回到锁定态。

不使用 localStorage，避免文章访问凭证长期保留；不只放内存，避免刷新页面后马上丢失。

### 评论授权

PASSWORD 文章评论读取和提交复用同一个 Article Access Token：

- `GET /api/public/articles/{articleId}/comments`
- `POST /api/public/articles/{articleId}/comments`

请求头：

```http
X-Article-Token: <articleToken>
```

无有效 token 时返回 `403 + 10003`。PUBLISHED 文章评论不需要该 token。

## 三端变更范围

### 后端

- 新增 PASSWORD 文章解锁接口。
- 新增 Article Access Token 签发与校验服务。
- 公开文章详情支持读取 `X-Article-Token`：
  - 无有效 token 时返回锁定元数据。
  - 有效 token 时返回正文。
- 文章评论列表和提交接口对 PASSWORD 文章校验 `X-Article-Token`。
- 增加解锁限流，复用可信客户端 IP 解析。
- API 文档补解锁接口、token header、错误码和过期语义。

### 前台

- 文章详情页识别 `locked=true`，显示锁定态和密码输入。
- 解锁成功后保存 token 到 `sessionStorage`，重新请求文章详情。
- PASSWORD 文章评论列表和提交请求附带 `X-Article-Token`。
- token 过期或无效时清理缓存，提示重新输入密码。
- 不把文章 token 放入 URL、日志或第三方 SDK。

### 后台

- 已有 PASSWORD 文章创建/编辑密码能力继续保留。
- 后台无需读取文章明文密码或 hash。
- 可在文章编辑页提示：PASSWORD 文章对读者端需要密码解锁后才能查看正文和评论。

## 验证建议

- 后端测试：PASSWORD 文章无 token 详情返回 `locked=true` 且 `body=null`。
- 后端测试：解锁密码正确时返回 article token。
- 后端测试：密码错误返回 `401 + 20002`。
- 后端测试：article token 不能访问其它文章。
- 后端测试：后台 access token 不能当作 article token。
- 后端测试：article token 不能当作后台 access token。
- 后端测试：PASSWORD 评论列表和提交无 token 返回 `403 + 10003`，有 token 成功。
- 前台测试：锁定态、解锁成功、token 过期清理、刷新后同一 tab 继续访问。
