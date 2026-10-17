# ForeverClient 构建部署教程

> 从源�?+ 注入 class 构建出完整可用的 `Forever-1.3.0.jar`，并部署到测试实例�?> 详细参考：`CLAUDE.md` §10（构建流程）、�?6（常见坑）�?
---

## 0. 环境要求

| �?| 要求 |
|---|---|
| 系统 | Windows |
| JDK | **21**（Minecraft 自带�?`java-runtime-delta`�?|
| 项目 | `src/`（源码）+ `inject/`（注�?class�? 构建配置 |

## 1. 设置 JDK

项目用的�?**Minecraft 运行时自带的 JDK 21**，JAVA_HOME 默认没设，必须先指定�?
```bash
export JAVA_HOME="C:/Users/kio22/AppData/Roaming/.minecraft/runtime/java-runtime-delta"
export PATH="$JAVA_HOME/bin:$PATH"
```

> 用错 JDK（如 Java 8）Gradle 会直接失败。`jdk21.zip`�?53 字节那个）是下载失败的空文件，别用�?
## 2. 构建 jar

```bash
cd /e/LyraeHack && ./gradlew build
```

- 产物：`build/libs/Forever-1.3.0.jar`（约 85MB�?- 耗时�?20~40s（首次下载依赖会更久�?
## 3. ⚠️ 注入混淆类（必须�?
源码里多数模块只�?参�?（反混淆出来的，逻辑残缺）�?*真正的实现在�?jar �?`.class` �?*，构建后必须�?`inject/` 目录注入�?jar�?
```bash
cd /e/LyraeHack/inject && "$JAVA_HOME/bin/jar" uf /e/LyraeHack/build/libs/Forever-1.3.0.jar dev/
```

**两个致命�?*（都踩过，造成真实事故）：

1. **必须 `cd /e/LyraeHack/inject` 再执�?*。在仓库根目录跑 `jar uf ... dev/` �?*静默 no-op**（根目录没有 `dev/`），导致所有注入模块（AutoEZ、StrafeSpeed、Punctuation、CryptUtil、SpearAura…）回退成损坏的源码编译版�?2. 注入�?jar 才会是完整可用状态�?
## 4.（可选）嵌入完整�?SHA

```bash
cd /e/LyraeHack && ./gradlew embedSha
```

- 计算 jar �?SHA-256 写入 `sha256.txt` 嵌入，运行时 `IntegrityCheck` 校验不一致会崩溃
- 不跑这步 �?jar �?`sha256.txt` �?校验跳过（开发构建安全）

## 5. 部署到测试实�?
⚠️ **游戏必须已关闭！** 运行中的 JVM 锁着�?jar，热替换会造成 `ZipFile invalid LOC header` 崩溃�?
```bash
cp -f /e/LyraeHack/build/libs/Forever-1.3.0.jar \
  "E:/Forever V2 SleepingPill/versions/Forever V2 SleepingPill/mods/Forever-1.3.0.jar"
```

校验一致性：

```bash
md5sum /e/LyraeHack/build/libs/Forever-1.3.0.jar \
  "E:/Forever V2 SleepingPill/versions/Forever V2 SleepingPill/mods/Forever-1.3.0.jar"
```

## 6. 常见问题

| 现象 | 原因 | 解决 |
|---|---|---|
| `ZipFile invalid LOC header` 崩溃 | 游戏运行时覆盖了 jar | 关游�?�?部署 �?重开 |
| 注入后模块还是坏�?| `jar uf` 在错误目录执�?| 必须 `cd inject/` |
| 编译失败（Java 8 错误�?| JAVA_HOME 没设/设错 | �?`java-runtime-delta` JDK 21 |
| `Follower` 不注�?| Follower 是源码编译，勿注�?| 保持源码�?|
| SpearAura 显示异常 | 注入源错�?| �?`Forever-airfix-v2-spear.jar` �?class |

## 7. NameTags 身份前缀（`[Dev]/[Beta]/[owner]`）说�?
本次新增功能，涉及客户端 + 后端两部分：

**客户�?*（已编译�?jar）：
- `NameTags.java` + `MixinPlayerListHud.java`：头�?/ Tab 列表显示前缀
- `ChatClient.java`：独立线程每 30s �?`/chat/users`，上报当前号 MC �?- 前缀规则：FlowerASleep→`§l§6[Dev]`（金色粗）、其余强制好友→`§9[Beta]`（蓝）、token 用户→`§d[owner]`（粉�?
**后端**（`patches/index.php`，需手动部署到服务器 `web/api/index.php`）：
- 新增 `/chat/users` route：KeyAuth token 鉴权，返�?owner 名单 + `mc_map`（MC名→owner，支持多开共用 token�?- 客户端用已登录的 `forever-token.json` 拉取，无需登录 IRC 账号
