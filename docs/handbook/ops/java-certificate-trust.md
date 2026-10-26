# Java 证书信任库维护

> 状态：当前有效；实现与部署结果须分别核对
> 适用范围：Java 17 构建镜像、运行镜像和三个后端 CI job
> 最后校准：2026-10-26
> 对应代码：`MyBlog-springboot-v2/Dockerfile`、`MyBlog-springboot-v2/scripts/security/`、`.github/workflows/ci.yml`
> 权威程度：安全维护手册

## 处置边界

CVE-2026-91403 涉及 eMudhra 证书信任，不是业务代码的远程执行缺陷。遵循 [Oracle 指引](https://ops.java/emudhra)：移除 `emSign ECC Root CA - G3`、`AAA Certificate Services`，以及指引列出的 `emSign Root CA - G1/G2`。后两者是扩大撤信范围，不应描述成其私钥也已失陷。不能仅依赖中间证书精确指纹黑名单，也不关闭 TLS 验证。

核对时 Temurin Linux `17.0.21+9` 已发布，但项目的 Maven/JRE Docker 标签尚未包含完整新发行版，因此在镜像构建时使用 JDK 自带 `keytool` 执行官方临时方案。三个后端 CI job 固定到该 Linux JDK 版本，并在任何 Maven 调用前执行同一检查/处置。基础镜像版本、证书内容和官方发行进度必须分别核实，不能把浮动的 `17` 标签视为已修复证据。

- `build-trust` 在 Maven 访问依赖仓库前处理构建 JDK。
- `runtime-trust` 处理运行 JRE，检查成功后删除构建工具；应用容器不在启动时修改信任库，不引入自定义 TrustManager。
- `distrust-emudhra.sh` 仅支持仓库使用的 Oracle/Temurin 标准 `cacerts`（默认密码 `changeit`），不用于带业务密钥的自定义 keystore。未知别名会由检查器发现并失败，不能忽略错误。
- `CheckCerts.java` 原样保留自 [Oracle/OpenJDK 检查器](https://cr.openjdk.org/~mr/CheckCerts.java)，保留其 BSD 许可证；按信任管理器实际接受的证书主体检查，不只检查已知别名。
- API 发布构建启用 `pull: true`；删除根证书的步骤仍须存在，不把重新拉取镜像当作安全验证。

## 验证

从仓库根目录执行；`JAVA_HOME` 必须指向待检查 JDK。离线测试只修改 `target` 下自动生成的测试 keystore，不修改本机 JDK：

```bash
bash MyBlog-springboot-v2/scripts/security/distrust-emudhra-test.sh
docker build --pull --target build-trust -t myblog-java-build-trust:test MyBlog-springboot-v2
docker build --pull --target runtime-trust -t myblog-java-runtime-trust:test MyBlog-springboot-v2
mvn -f MyBlog-springboot-v2/pom.xml clean test
```

离线测试覆盖 Oracle/Temurin 别名、四类目标证书、正常证书保留、重复执行不改写、缺失/损坏/错误密码失败，以及未知别名的问题证书不能绕过检查。真实 MySQL 专项仍由现有 CI 执行，不能把本地无 Docker 时的跳过算作通过。

可用 `javac -encoding UTF-8 MyBlog-springboot-v2/scripts/security/CheckHttps.java` 编译，并在待发布 JRE 上执行 `java -cp <class目录> CheckHttps <accept|reject> <https-url>` 做无凭据握手冒烟：`accept` 检查正常 Maven Central、S3 和邮件 API 主机；`reject` 检查已确认仍展示问题证书链的测试主机。DNS/连接超时或普通 TLS 错误不计为撤信成功。外部测试站可能变化，不能替代离线根证书检查。此检查不上传附件、不发送邮件，不等同于真实业务操作验收。

发布前必须取得全套 CI 成功证据；部署后另在实际 API 容器的 JRE 中运行编译后的 `CheckCerts`，核对运行镜像 SHA、容器健康和正常外连。检查器只临时放入容器，不修改其信任库。证书状态等生产细节只写入私有台账，不写入此文档。

## 移除临时方案与回滚

当构建、运行和 CI 的官方 JDK 都已自带撤信修复时，更新至经验证的明确版本/镜像摘要；验证未运行处置脚本的原始镜像也能通过 `CheckCerts`，再独立删除处置脚本及其专用测试，保留必要的证书检查。不要无期限保留补丁，也不要仅凭版本号提前删除。

本次不修改数据库、业务逻辑、网站叶子证书、Caddy/Go 或宿主机 CA。镜像回滚会连同旧信任库一起恢复，因此旧 SHA 不能作为安全完成状态；如因外连兼容问题回滚，必须继续跟踪撤信修复，不重新导入受影响根证书来绕过验证。
