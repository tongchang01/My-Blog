# MyBlog

[English](./README.en.md) | [日本語](./README.ja.md) | [简体中文](./README.zh-CN.md)

MyBlog は、以下の 3 つで構成されたフルスタックのブログシステムです。

- 公開用フロントエンド：`MyBlog-blog`
- 管理画面：`MyBlog-admin`
- Spring Boot バックエンド：`MyBlog-springboot`

## プロジェクト構成

```text
E:\My-Blog
├─ MyBlog-vue
│  ├─ MyBlog-blog
│  └─ MyBlog-admin
└─ MyBlog-springboot
```

## 技術スタック

### フロントエンド `MyBlog-blog`

- Vue 3
- TypeScript
- Pinia
- Vue Router 4
- Element Plus
- Tailwind CSS
- APlayer

### 管理画面 `MyBlog-admin`

- Vue 2
- Vuex
- Vue Router 3
- Element UI
- mavon-editor
- ECharts

### バックエンド `MyBlog-springboot`

- Java 8
- Spring Boot 2.3.7
- Spring Security
- MyBatis-Plus
- MySQL
- Redis
- RabbitMQ
- Quartz
- Knife4j / Swagger 2
- AWS S3 / SES

## 主な機能

- ブログ公開ページ
- 記事 / カテゴリ / タグ管理
- コメント管理と審査
- つぶやき、アルバム、リンク管理
- ユーザー / ロール / リソース / メニュー権限管理
- 定期実行ジョブとログ管理
- サイト設定管理
- 音楽プレイヤーと楽曲管理

## ローカル実行

### ブログフロント

ディレクトリ：`E:\My-Blog\MyBlog-vue\MyBlog-blog`

```bash
npm install
npm run serve -- --port 8081
npm run build
```

### 管理画面

ディレクトリ：`E:\My-Blog\MyBlog-vue\MyBlog-admin`

```bash
npm install
npm run serve -- --port 8082
npm run build
```

### バックエンド

ディレクトリ：`E:\My-Blog\MyBlog-springboot`

```bash
mvn clean package
mvn spring-boot:run
```

デフォルトのバックエンドポート：`8080`

両方のフロントエンドは `/api` を以下へプロキシします。

- `http://localhost:8080`

## データベース初期化

SQL ディレクトリ：`E:\My-Blog\MyBlog-springboot\sql`

- [aurora.sql](./MyBlog-springboot/sql/aurora.sql)：基本スキーマと初期データ
- [music-player.sql](./MyBlog-springboot/sql/music-player.sql)：音楽プレイヤー機能の追加スクリプト

推奨順序：

1. データベース作成
2. `aurora.sql` をインポート
3. 音楽機能が必要なら `music-player.sql` を追加でインポート

## バックエンド設定

設定ファイル：

- [application.yml](./MyBlog-springboot/src/main/resources/application.yml)
- [application-dev.yml](./MyBlog-springboot/src/main/resources/application-dev.yml)
- [application-local.yml](./MyBlog-springboot/src/main/resources/application-local.yml)

確認すべき項目：

- MySQL
- Redis
- RabbitMQ
- JWT
- AWS S3 / SES
- SMTP メール設定
- サイトドメイン
- QQ ログイン設定

## `.env` 変数

実際のシークレットは README に書かず、ローカル環境変数または `.env` で管理することを推奨します。

```env
AWS_S3_KEY=your_s3_access_key
AWS_S3_SECRET=your_s3_secret
AWS_S3_URL=https://your-bucket.s3.your-region.amazonaws.com/
AWS_S3_BUCKET=your_bucket_name
AWS_S3_REGION=your_region

AWS_SES_KEY=your_ses_access_key
AWS_SES_SECRET=your_ses_secret
AWS_SES_FROMEMAIL=your_sender_email
AWS_SES_REGION=your_ses_region
AWS_SES_DOMAIN=your_domain

DBPASSWORD=your_database_password
JWT_SECRET=your_jwt_secret
```

各変数の用途：

- `AWS_S3_KEY` / `AWS_S3_SECRET`：S3 認証情報
- `AWS_S3_URL` / `AWS_S3_BUCKET` / `AWS_S3_REGION`：S3 ストレージ設定
- `AWS_SES_KEY` / `AWS_SES_SECRET`：SES 認証情報
- `AWS_SES_FROMEMAIL` / `AWS_SES_REGION` / `AWS_SES_DOMAIN`：SES メール設定
- `DBPASSWORD`：DB と Redis で使うパスワード変数
- `JWT_SECRET`：JWT 署名用シークレット

## 音楽プレイヤーのカスタマイズ

現在の挙動：

- 悬浮ドック表示のみ
- 右下固定モードは削除
- デスクトップ表示位置を上へ移動し、右下 UI と重ならないよう調整

## 紹介記事

- [English Showcase](./docs/MyBlog-Project-Showcase.en.md)
- [日本語 紹介記事](./docs/MyBlog-プロジェクト紹介.ja.md)
- [中文项目展示](./docs/MyBlog-项目展示.md)
