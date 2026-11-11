# 破碎的像素地牢网页版

基于官方开源 **v4.0.2** 的非官方浏览器移植。保留官方游戏逻辑与资源，通过 libGDX TeaVM 后端运行；没有使用社区网页复刻替代官方游戏代码。

官方源：https://github.com/00-Evan/shattered-pixel-dungeon

基线提交：`57a4e06a4caf162446d1c28caa7983f0493fecf0`。

## 当前功能

- 默认纯游戏界面，浏览器标题为“破碎的像素地牢”。
- 首次进入默认简体中文，保留原版教程。
- 初始解锁六名角色和炼金指南页面；制作材料与消耗继续遵循游戏规则。
- 单份浏览器本地存档；`/?manage=1` 提供导出、导入和备份恢复。
- 每层检查点；死亡后原生游戏窗口提供重试、导出、返回菜单或结束本局。
- 本层重试与主菜单切换在游戏内部完成，不重新加载页面。
- 未加入账号、多人游戏或服务器云存档。

## Windows 构建

已验证环境：Git、JDK 21、Node.js、Gradle 9.4.0。源码包不包含这些工具。将 JDK 的路径设置到 `JAVA_HOME`，并将 Node.js、Git、Gradle 放入 PATH。

在仓库根目录的 PowerShell 运行：

```powershell
node web/tools/audit-upstream.mjs
$jsTests = @((Get-ChildItem web/tests -Filter '*.test.mjs').FullName)
node --test --test-isolation=none @jsTests
gradle -p web test buildWeb --no-daemon --console=plain
node --check web/build/dist/webapp/dungeon.js
node web/tools/package.mjs
```

每条命令必须成功后再执行下一条。审计要求本地保留官方 `v4.0.2` 标签，具体导入方式见上传包的说明。

`web/tools/build.ps1` 也会执行完整流程；它优先使用仓库旁 `.tools/` 中的便携工具，未找到时使用官方 Gradle wrapper。官方 wrapper 当前声明 9.5.0，本文列出的手动流程使用实测通过的 9.4.0。

构建输出：`web/build/public/`。其中 `source.zip` 是对应构建源码，`play/version.json` 记录官方基线和源码校验值。不要删除这些公开源码和许可证文件。

## 本地启动

```powershell
$env:STATIC_ROOT = (Resolve-Path web/build/public).Path
$env:HOST = '127.0.0.1'
$env:PORT = '8082'
node hosting/server.mjs
```

打开 `http://127.0.0.1:8082/`，存档管理位于 `http://127.0.0.1:8082/?manage=1`。停止服务按 Ctrl+C。

## 宿主机部署

见 [hosting/README.md](hosting/README.md)。已编译的部署包另行提供，不必在 2GB 内存的服务器上编译 Java。

## 合并官方更新

保持当前工作区已提交，并确认 `upstream` 指向官方仓库：

```powershell
git remote get-url upstream
powershell -File web/tools/update-upstream.ps1
```

也可以通过 `-Tag` 指定已经存在的更高稳定版本。脚本建立更新分支、合并官方版本并运行审计和构建；冲突或验证失败时停下供维护者处理。不会自动部署。

更换游戏版本前，导出旧档，在测试浏览器验证续玩、切层、背包、菜单、死亡重试及导出导入。升级后的存档不要交给旧版程序；回滚时同时恢复升级前备份。

## 目录

| 路径 | 用途 |
| --- | --- |
| `core/`、`SPD-classes/` | 官方共享游戏代码与浏览器所需兼容改动 |
| `web/src/` | TeaVM 入口、浏览器平台与存档桥接 |
| `web/portal/` | 游戏外层页面、存档封装与管理 |
| `web/tests/` | 浏览器启动、存档与 UUID 回归测试 |
| `web/patches/compatibility.patch` | 官方代码兼容修改的审计基线 |
| `web/tools/` | 构建、源码打包、审计、上游更新 |
| `hosting/` | 无 Docker 的 Node.js 静态服务和 systemd 部署脚本 |
| `WEB-PORT.md` | 移植原理、存档格式、限制与更新说明 |

## 注意事项与许可

浏览器存档与站点地址绑定；改变域名、协议或端口前先导出。不同浏览器和设备不会自动同步。当前档案上限为 2MiB，深层长局仍需更多测试。

本项目按仓库 `LICENSE.txt` 的 GPLv3 或更新版本提供，保留 Evan Debenham、Watabou 及相关开源组件署名。发布网页运行代码时应一并提供对应修改源码。原项目 README 保留在 `README.md`。
