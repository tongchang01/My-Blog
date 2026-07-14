# MyBlog V2

[简体中文](README.md) | [English](README.en.md) | **日本語**

MyBlog V2 は、Spring Boot API、公開ブログ、独立ビルド可能な管理画面で構成されるモジュラーモノリス型の個人ブログシステムです。現在の開発対象は V2 で、[公開ブログ](https://tong-yibin.com) と [管理画面](https://admin.tong-yibin.com) はすでに本番稼働しています。V1 の履歴コードは読み取り専用の `archive/v1-master-2026-06-26` ブランチを参照してください。

## 現在の機能

- 3 言語対応の公開ブログ：ホーム、記事、カテゴリ、タグ、アーカイブ、検索、About、メッセージボード、リンク、記事コメント。
- ホーム編集：固定記事 1 件、注目記事最大 2 件、通常記事一覧。
- 管理画面：記事、カテゴリ・タグ、コメント、リンク、添付ファイル、サイト設定、著者プロフィール、アクセス統計。
- ADMIN/DEMO 権限、JWT access token、DB refresh token のローテーション、ログアウト、パスワード変更による失効。
- Markdown、コメント HTML のサニタイズ、Flyway V1–V4、14 テーブル、論理削除、監査項目。
- local/test/prod profile、LOCAL/S3 ストレージ、health endpoint、GitHub Actions CI/CD、GHCR、Docker Compose デプロイ。

PASSWORD 記事の解除機能は未実装です。`main` の公開では、コミット SHA タグ付きの GHCR イメージを作成し、AWS EC2 へ自動デプロイします。デプロイ後は公開 HTTPS ヘルスエンドポイントのスモークテストを実行します。未解決事項は[未解決事項](docs/handbook/start-here/open-issues.md)を参照してください。

## アーキテクチャ

バックエンドのルートパッケージは `com.tyb.myblog.v2` です。identity、content、comment、system、stats の 5 業務モジュールが web/application/domain/infrastructure の 4 層を持ちます。モジュール間の呼び出しは application 契約を経由し、ArchUnit が依存方向を検証します。common はセキュリティ、ストレージ、メール、時刻、Web、永続化の共通基盤です。

| 領域 | 技術 |
| --- | --- |
| バックエンド | Java 17、Spring Boot 3.5、Spring Security、MyBatis-Plus、Flyway |
| データ・コンテンツ | MySQL 8、CommonMark、OWASP HTML Sanitizer、Caffeine |
| ストレージ・メール | ローカル / AWS S3、任意の Resend |
| ブログ | Vue 3、Pinia、Vue Router、vue-i18n、markdown-it、Vite、Vitest |
| 管理画面 | Vue 3、Pinia、Element Plus、ECharts、Vite、Vitest |
| 配布・デプロイ | Docker Compose、Caddy、GitHub Actions、GHCR、AWS EC2 / Route 53 / S3 |
| テスト | JUnit 5、Spring Boot Test、ArchUnit、H2、Testcontainers |

V2 は Redis、RabbitMQ、Elasticsearch、Quartz に実行時依存しません。Caffeine のレート制限は単一バックエンドインスタンスを前提としています。

## ディレクトリ

```text
MyBlog-springboot-v2/   V2 バックエンド
frontend/apps/blog/     V2 公開ブログ
frontend/apps/admin/    V2 管理画面
docs/                   現行ドキュメントとガバナンス
```

## ローカル起動

JDK 17、Maven 3.9.x、MySQL 8、Node 24+、pnpm 9、JVM 時区 `Asia/Tokyo` が必要です。

```powershell
# Backend
cd MyBlog-springboot-v2
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Blog: http://localhost:5173
cd frontend/apps/blog
corepack pnpm install --frozen-lockfile
corepack pnpm dev

# Admin: http://localhost:8848
cd frontend/apps/admin
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

バックエンドには DB、JWT、統計用の環境変数が必要です。[ローカル起動](docs/handbook/ops/local-development.md)、[環境変数](docs/handbook/ops/environment.md)、[MySQL 手順](docs/handbook/ops/local-mysql-development.md)を参照してください。

## 検証

```powershell
cd MyBlog-springboot-v2
mvn clean test

corepack pnpm --dir frontend/apps/blog test
corepack pnpm --dir frontend/apps/blog typecheck
corepack pnpm --dir frontend/apps/blog build

corepack pnpm --dir frontend/apps/admin test
corepack pnpm --dir frontend/apps/admin typecheck
corepack pnpm --dir frontend/apps/admin build
```

## ドキュメント

- [ドキュメント入口](docs/README.md)
- [開発ハンドブック](docs/handbook/README.md)
- [API 契約](docs/handbook/api/README.md)
- [プロダクト仕様](docs/handbook/product/README.md)
- [運用とリリース](docs/handbook/ops/README.md)
- [本番デプロイ方針](docs/handbook/ops/deployment-direction.md)

## License

本リポジトリは [MIT License](LICENSE) です。ブログは [Hexo Theme Aurora](https://github.com/auroral-ui/hexo-theme-aurora) を起点とし、管理画面は [pure-admin-thin](https://github.com/pure-admin/pure-admin-thin) を基盤としています。各アプリの上流ライセンスと出典記録は保持されています。
