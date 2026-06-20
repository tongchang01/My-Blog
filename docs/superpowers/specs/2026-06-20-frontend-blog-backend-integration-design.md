# 博客前台 V2 首批后端联调设计

## 1. 目标

在保留 Aurora 现有视觉与组件基础的前提下，让 `frontend/apps/blog/` 脱离 Hexo 静态 JSON 数据契约，并完成第一个可独立验收的真实后端纵向切片：

1. 根路径按用户语言偏好进入三语路由。
2. 从 V2 后端加载公开站点配置。
3. 首页加载真实文章列表并支持分页。
4. 进入非 PASSWORD 文章详情。
5. 对 PASSWORD、404、网络错误和空列表提供明确页面状态。

本批次不对接分类、标签、归档、搜索、About、友链、评论、留言板和访问打点；这些能力按相同架构后续分批迁移。

## 2. 产品与版本边界

### 2.1 首个里程碑

同时启动前台和后端后，访客可以：

- 进入按语言分段的首页；
- 看到后端返回的站点标题、副标题和真实文章列表；
- 翻页浏览文章；
- 进入非 PASSWORD 文章详情；
- 从缺失或过期 slug 的 URL 自动进入 canonical URL；
- 在受限或失败场景看到正确状态，而不是空白页或伪空数据。

### 2.2 PASSWORD 边界

首版后端尚未提供 PASSWORD 解锁接口。本批次只实现锁定占位态：

- 列表按后端 `locked` 字段展示锁定状态；
- 详情收到 `HTTP 403 + code="10003"` 时展示锁定页；
- 不创建密码输入框、解锁 API、临时 token 或前端伪解锁逻辑。

## 3. 渐进式脱离 Hexo

“脱离 Hexo”指切断前台对 Hexo 生成数据格式和静态 JSON API 的依赖，不是替换 Aurora UI。

当前 `public/api/site.json` 同时混合 Hexo 顶层字段、主题展示配置和站点业务内容。迁移后拆成：

- **后端动态配置**：站点标题、副标题、About Markdown、Logo、Favicon、ICP备案和 Spotify ID，来自 `/api/public/site-config`。
- **前端默认配置**：主题色、导航菜单、作者资料、社交链接、布局开关和首页动画文字，当前保留在类型明确的源码配置中。
- **未来后台配置**：主题色、导航、作者资料、社交链接和展示开关后续可逐项扩展后端 Schema、管理 API 和后台页面。本批次不预建任意 key-value 配置中心。

前台组件只消费合并后的 `SiteSettingsViewModel`，不感知字段来自 API 还是默认配置。未来后台接管某项配置时，只替换数据来源，不再次修改所有消费组件。

## 4. 代码组织

首批采用按业务纵切的结构：

```text
frontend/apps/blog/src/
├─ shared/
│  ├─ http/
│  │  ├─ client.ts
│  │  ├─ contract.ts
│  │  └─ error.ts
│  ├─ i18n/
│  │  └─ locale.ts
│  └─ time/
│     └─ jst.ts
└─ features/
   ├─ site-settings/
   │  ├─ api.ts
   │  ├─ contract.ts
   │  ├─ defaults.ts
   │  ├─ mapper.ts
   │  ├─ model.ts
   │  └─ store.ts
   └─ articles/
      ├─ api.ts
      ├─ contract.ts
      ├─ mapper.ts
      ├─ model.ts
      └─ store.ts
```

依赖方向固定为：

```text
后端 DTO -> mapper -> ViewModel -> Pinia Store -> 页面与组件
```

约束：

- 页面和组件不得直接依赖 Axios 响应或 `ApiResponse<T>`。
- `contract.ts` 只描述后端契约；`model.ts` 只描述页面语义。
- mapper 集中处理分页字段、空值、封面、锁定状态和 JST 时间。
- 旧 `Post.class.ts` 只服务尚未迁移页面，不新增依赖；首批页面迁移后不再通过它构造文章数据。
- 不提前创建后台应用或共享 packages。后台启动后，再把已经稳定且确实复用的契约与 HTTP 能力抽到 workspace package。

## 5. HTTP 与契约处理

### 5.1 业务 ID 的 JSON 表达

后端内部、数据库和路径参数继续使用 Java `long` / MySQL `BIGINT`，但所有进入本批次公开文章响应的业务 ID 必须输出为 JSON string，包括文章 `id`、`categoryId` 和标签 `id`。原因是 `ASSIGN_ID` 雪花值可能超过 JavaScript 的安全整数范围；前端不得先接收 number 再调用 `String()`，因为精度在 JSON 解析时已经丢失。

本批次先收窄修复公开文章列表与详情契约，并同步后端测试和 API 文档。其他后台或后续公开接口的 ID 字符串化按对应前端联调批次处理，不在本轮全量扩散。前端 contract、ViewModel、路由参数和 API 参数从一开始都把业务 ID 建模为 `string`。

### 5.2 HTTP client

`shared/http/client.ts` 负责：

- 使用环境变量配置 API base URL；
- 统一 5 秒超时；
- 解包 `{ code, msg, data }`；
- 只有 `code === "00000"` 才返回业务数据；
- 将 HTTP 状态、业务 code、msg 和网络错误规范化为 `ApiError`；
- 支持 `AbortSignal`，用于取消语言切换和分页产生的旧请求；
- 不在底层直接弹 UI 提示。

本地开发通过 Vite proxy 将 `/api` 转发到后端，生产环境通过同源反向代理或显式 API base URL 部署。前台不硬编码后端主机、端口或凭据。

## 6. 站点配置数据流

```text
typed defaults
    + GET /api/public/site-config?lang={lang}
    -> mapSiteSettings
    -> SiteSettingsViewModel
    -> site settings store
    -> header / home / footer / document metadata
```

后端请求成功时，已支持字段覆盖默认值；后端可选字段为 `null` 时保留明确的空语义，不把所有空值都错误替换成默认文案。只有尚未进入后端契约的展示配置始终来自 defaults。

站点配置请求失败时，前台使用 defaults 继续渲染，并显示非阻塞提示。配置失败不能阻断文章列表请求。

## 7. 文章数据流

文章列表调用：

```text
GET /api/public/articles?page={page}&size={size}&lang={lang}
```

首批固定 `size=12`。mapper 将后端 `records/total/page/size/pages` 映射为首页分页 ViewModel，并将文章项映射为卡片所需字段。页面不再依赖 Aurora 的 `PostList` 构造器。

文章详情调用：

```text
GET /api/public/articles/{id}?lang={lang}
```

详情 ViewModel 包含标题、摘要、正文、分类、标签、封面、发布时间、创建/更新时间、slug 和锁定状态所需语义。Markdown 渲染安全策略不在本批次扩大；详情先沿用现有可信作者内容渲染链路，评论仍禁止直接渲染 `content_md`。

## 8. 三语与路由

支持语言固定为 `zh`、`ja`、`en`。

根路径 `/` 的选择顺序：

1. 已保存的用户选择；
2. 浏览器系统语言：`zh-* -> zh`、`ja-* -> ja`、其他语言 -> `en`。

用户主动切换语言后保存选择。后端日语或英语字段为空时，接受后端按字段回退中文的既有契约。

路由：

```text
/:lang
/:lang/posts/:id/:slug?
```

文章查询键始终是 `id`。详情加载成功后比较响应 slug：URL 缺少 slug 或不一致时使用 router `replace` 修正为 canonical URL，不产生额外历史记录。非法语言进入 404，不向后端发送非法 `lang`。

## 9. JST 时间语义

后端 `LocalDateTime` 字符串不携带 offset，但语义固定为 Asia/Tokyo。`shared/time/jst.ts` 负责显式按 JST 解释和格式化，不允许直接依赖浏览器本地时区解析。

首批列表和详情统一通过该工具生成日期展示模型；不得在 mapper、组件和页面中重复拼接或自行 `new Date(raw)`。

## 10. 页面状态与错误处理

### 10.1 首页

- 加载中：显示现有骨架屏。
- 成功且有数据：显示文章卡片和分页。
- 成功但无数据：显示独立空状态。
- 请求失败：显示错误面板与重试按钮，不伪装为空列表。

### 10.2 详情

- `403 + 10003`：PASSWORD 锁定占位页。
- 404：文章不存在页。
- 网络错误或 5xx：保留当前 URL，显示可重试错误状态。
- 请求取消：静默结束，不覆盖当前页面状态。

HTTP client 只提供结构化错误，具体文案和交互由 feature/store/page 决定。

## 11. 实施批次与提交边界

### 批次 0：公开文章 ID 契约前置修复

- 公开文章列表与详情的文章、分类和标签 ID 输出为 JSON string。
- 后端内部查询、持久化和 path variable 保持 `long`。
- 同步公开文章控制器/OpenAPI 测试和 `api-contract/article.md`。

### 批次 1：前端质量基线

- 修复现有 10 个 TypeScript 错误。
- 正式引入 `vue-tsc` 和 `typecheck` 命令。
- 引入 Vitest 基线。

### 批次 2：共享基础能力

- HTTP client、统一响应解包和 ApiError。
- 语言选择、路由语言校验和持久化。
- JST 时间工具。

### 批次 3：站点配置纵向切片

- 默认配置、后端契约、mapper、store。
- App 启动加载和失败降级。
- 将当前仍有效的主题、导航、作者和社交配置完整迁入 typed defaults 后，删除 `site.json` 读取链路；其他 mock 暂留。

### 批次 4：公开文章列表纵向切片

- 列表 DTO、ViewModel、mapper、store。
- 首页、卡片、分页、空状态、错误重试。
- 删除 `posts/1.json` 和首页 feature 静态读取链路；首页特色区暂按普通列表数据降级，不在本批次发明后端不存在的“精选文章”契约。

### 批次 5：公开文章详情纵向切片

- id-led 路由与 canonical slug。
- 详情 DTO、ViewModel、mapper、store。
- PASSWORD、404、网络失败状态。

每个批次独立验证并使用一个或多个单一目的中文提交；不得把分类、评论或后台工程顺带带入。

## 12. 测试与验收

### 12.1 自动验证

- Vitest：语言选择与回退、ApiResponse 解包、错误分类、JST 解析、站点配置合并、文章列表/详情 mapper。
- Store 测试：加载、空数据、失败、取消旧请求和重试状态。
- 后端测试：以超过 JavaScript 安全整数的 ID 验证公开文章列表与详情响应中的业务 ID 均为 JSON string。
- `pnpm lint`。
- `pnpm typecheck`。
- `pnpm test --run`。
- `pnpm build`。

### 12.2 真实联调

启动后端 local Profile 和前台后验证：

1. `/` 按已保存语言或系统语言跳转。
2. `/:lang` 加载真实站点配置和文章列表。
3. 首页分页请求与页码一致。
4. 切换语言后请求使用新的 `lang`，旧请求结果不会覆盖页面。
5. 非 PASSWORD 文章详情可打开。
6. 缺失或错误 slug 被 canonical replace 修正。
7. PASSWORD 文章显示锁定占位。
8. 404、后端停止和 5xx 显示正确错误状态。

## 13. 后续批次

首个里程碑通过后，按以下顺序另写设计或计划：

1. 分类、标签、归档和搜索。
2. About 与友链。
3. 评论和留言板。
4. 访问打点。
5. 后台工程骨架与站点配置管理。
6. 扩展后端 Schema 与后台 UI，逐项接管主题、导航、作者和社交配置。
