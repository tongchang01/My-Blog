FROM node:24-alpine AS blog-build

WORKDIR /app
RUN corepack enable && corepack prepare pnpm@9.15.9 --activate
COPY frontend/apps/blog/package.json frontend/apps/blog/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY frontend/apps/blog/ ./
RUN pnpm build

FROM node:24-alpine AS admin-build

WORKDIR /app
RUN corepack enable && corepack prepare pnpm@9.15.9 --activate
ENV HUSKY=0
COPY frontend/apps/admin/.npmrc frontend/apps/admin/package.json frontend/apps/admin/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY frontend/apps/admin/ ./
RUN pnpm build

FROM caddy:2.11.4-alpine

COPY --from=blog-build /app/dist /srv/blog
COPY --from=admin-build /app/dist /srv/admin
COPY deploy/web/Caddyfile /etc/caddy/Caddyfile

EXPOSE 80 443
