import { gzipSync } from "node:zlib";
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const distDir = resolve(dirname(fileURLToPath(import.meta.url)), "../dist");
const html = readFileSync(resolve(distDir, "index.html"), "utf8");
const assets = [
  ...html.matchAll(/<script\b[^>]*\bsrc="([^"]+\.js)"[^>]*>/g),
  ...html.matchAll(/<link\b[^>]*\brel="stylesheet"[^>]*\bhref="([^"]+\.css)"[^>]*>/g)
].map(match => match[1]);

if (assets.length === 0) {
  throw new Error("未在 dist/index.html 中找到首屏 JS 或 CSS");
}

const sizes = assets.map(asset => {
  const file = resolve(distDir, asset.replace(/^\/+/, ""));
  return {
    asset,
    type: asset.endsWith(".js") ? "js" : "css",
    gzipBytes: gzipSync(readFileSync(file)).byteLength
  };
});

const totals = Object.groupBy(sizes, ({ type }) => type);
const gzipBytes = type =>
  (totals[type] ?? []).reduce((total, asset) => total + asset.gzipBytes, 0);
const actual = {
  js: gzipBytes("js"),
  css: gzipBytes("css"),
  total: gzipBytes("js") + gzipBytes("css")
};
const budget = {
  js: 480 * 1024,
  css: 80 * 1024,
  total: 560 * 1024
};
const kib = bytes => `${(bytes / 1024).toFixed(2)} KiB`;

for (const asset of sizes) {
  console.log(`${asset.asset}: ${kib(asset.gzipBytes)} gzip`);
}

const failures = Object.keys(budget).filter(type => actual[type] > budget[type]);
console.log(
  `首屏合计：JS ${kib(actual.js)} / CSS ${kib(actual.css)} / 总计 ${kib(actual.total)}`
);

if (failures.length > 0) {
  for (const type of failures) {
    console.error(`${type} 超出预算：${kib(actual[type])} > ${kib(budget[type])}`);
  }
  process.exitCode = 1;
}
