const { defineConfig } = require('@vue/cli-service')
const path = require('path')
function resolve(dir) {
  return path.join(__dirname, dir)
}
module.exports = defineConfig({
  transpileDependencies: true,
  productionSourceMap: false,
  publicPath: '/admin/',
  devServer: {
    proxy: {
      '/api': {
        // target: 'https://www.linhaojun.top/api', 代理当前指向作者服务器
        target: 'http://localhost:8080',//本地启动测试
        changeOrigin: true,
        pathRewrite: {
          '^/api': ''
        }
      }
    }
  },
  chainWebpack: (config) => {
    config.resolve.alias.set('@', resolve('src'))
  }
})
