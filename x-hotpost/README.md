# 热帖 · X 热点审核发布

自动跟踪热点 → 生成发帖草稿 → **你审核通过后**再发布。后台定时跑，不用一直盯着。

本目录是可本地运行的 Web 控制台（Vite + Express），和仓库里 Topic Radar 一样偏「先能跑、再接真 API」。

## 能做什么

1. **跟踪热点**：按赛道拉取公开 Google News RSS（演示模式还有内置样本）
2. **自动写草稿**：本地模板，或可选 OpenAI 兼容接口
3. **人工审核**：改文案 / 通过 / 拒绝；通过前不会发到 X
4. **后台发布**：审核通过后自动发帖（演示模式只记本地日志）
5. **变现钩子**：可配置联盟链接与 CTA 模板，写进草稿结尾

## 快速开始

```bash
cd x-hotpost
npm install
npm run dev
```

- 前端：http://127.0.0.1:5173
- API：http://127.0.0.1:8788

打开后点「立即跑一轮」，即可看到热点与待审草稿。

只跑一轮后台任务：

```bash
npm run tick
```

生产构建：

```bash
npm run build
npm start
```

## 接真 X 账号

1. 在 [developer.x.com](https://developer.x.com) 创建 Project/App
2. 打开 **Read and Write**，生成 **OAuth 1.0a** 的：
   - API Key / API Secret
   - Access Token / Access Token Secret（对应用户账号）
3. 在「设置」里填入上述四项
4. **关闭演示模式** 并保存
5. 审核通过一条草稿，确认真实发帖

> X API 当前多为按次计费。发帖、读趋势都会产生费用，请控制频率；本工具默认「先审后发」，避免失控刷屏。

## 变现怎么用

赚钱不靠“无脑群发”，靠的是稳定输出 + 信任 + 转化：

- 选准赛道（科技、财经、自媒体、自定义关键词）
- 在设置里填 **产品 / 联盟 / 社群** 链接与 CTA
- 每条热点给出你的判断，而不是纯搬运标题
- 保持审核：垃圾内容会伤账号权重，也伤变现

## 目录

```
x-hotpost/
  server/   # Express API、热点采集、草稿、OAuth1 发帖、定时任务
  src/      # React 审核台
  data/     # 本地 JSON（运行后生成，已 gitignore）
```

## 合规提醒

- 只用你自己有权使用的 X 账号与 API 凭证
- 遵守 [X 开发者协议](https://developer.x.com/en/developer-terms) 与平台规则
- 付费推广请按规定披露（可用 API 的 `paid_partnership` 等能力后续扩展）
