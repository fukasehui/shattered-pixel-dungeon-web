# 宿主机部署，不使用 Docker

游戏在浏览器运行；服务器仅提供静态资源。使用已编译部署包时无需 Java 或 Gradle。

## 快速检查

部署前检查端口、内存和已有服务：

```bash
free -m
ss -lntp
systemctl --type=service --state=running
```

服务默认使用 8082。若已被其他服务占用，先修改 `deploy.sh` 中的 `PORT`。不要停止不相关服务。

## 手动运行

服务器需要可用的 Node.js。解压发布包到一个新目录后，目录应包含 `public/`、`server.mjs` 和 `deploy.sh`：

```bash
HOST=0.0.0.0 PORT=8082 STATIC_ROOT=./public node server.mjs
```

该命令前台运行，适合验证。需要公网访问时，在服务器及云防火墙放行对应端口。游戏没有登录认证，玩家进度保存在各自浏览器中。

## systemd 后台运行和开机自启

`deploy.sh` 是针对现有目录布局的 root 部署脚本。默认 Node 路径为 `/opt/node/bin/node`；在其他服务器先用 `command -v node` 确认并修改脚本的 `ExecStart`。

例如使用本次已验证发布编号 `4.0.2-r3`：

```bash
mkdir -p /srv/pixel-dungeon-official/releases/4.0.2-r3
tar -xzf web-release-4.0.2-r3.tar.gz -C /srv/pixel-dungeon-official/releases/4.0.2-r3
bash /srv/pixel-dungeon-official/releases/4.0.2-r3/deploy.sh 4.0.2-r3
curl -I http://127.0.0.1:8082/
```

应在尚不存在或尚未使用的新发布目录中解压。已经存在 r3 时选用下一个修订号，避免覆盖正在运行的文件。

脚本建立 `pixeldungeon` 系统用户、配置 96MiB 内存限制、启用 `pixel-dungeon-official.service`，然后切换 `current` 软链接并重启该游戏服务。原发布通过 `previous` 保留。未来发布须使用新的目录编号。

## 常用管理命令

```bash
systemctl status pixel-dungeon-official
systemctl restart pixel-dungeon-official
systemctl stop pixel-dungeon-official
systemctl start pixel-dungeon-official
journalctl -u pixel-dungeon-official -n 100 --no-pager
readlink -f /srv/pixel-dungeon-official/current
readlink -f /srv/pixel-dungeon-official/previous
```

更新后应实际测试游戏。回滚时将 `current` 指向已验证的旧发布目录并重启服务；若已经迁移浏览器存档，同时在 `/?manage=1` 恢复升级前备份。

## 从源码生成发布包

在仓库完成构建后，新建一个发布目录，将 `web/build/public/` 复制为其 `public/`，再复制本目录的 `server.mjs` 和 `deploy.sh`。打包这个发布目录的内容即可，布局须与预编译发布包一致。

原样保留 `public/source.zip`、`public/LICENSE.txt` 及版本信息，供用户获取对应源码与许可证。
