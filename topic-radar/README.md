# Topic Radar

把指定网站/RSS 上关于某个话题的内容，聚合成一条可浏览的情报流。

预置示例话题：**温哥华房价**，默认源包括：

- Google News（英文 / 中文检索）
- CBC British Columbia
- Reddit `r/vancouverhousing`

## 怎么用

```bash
cd topic-radar
npm install
npm run dev
```

- 前端：http://127.0.0.1:5173
- API：http://127.0.0.1:8787

打开页面后点「立即采集」，即可拉取并过滤相关文章。

只跑采集（不启前端）：

```bash
npm run collect
```

生产构建：

```bash
npm run build
npm start
```

## 功能

- 多话题：每个话题有独立关键词与 RSS 源
- RSS 采集：公开 feed，比整站爬虫更稳、也更合规
- 关键词过滤：对综合新闻源按标题/摘要匹配
- 本地存储：结果保存在 `data/store.json`
- 可扩展：继续加 Google News 查询、媒体 RSS、社区 RSS

## 建议的信息源类型

优先用公开 RSS / 官方 feed，例如：

1. `https://news.google.com/rss/search?q=你的关键词&hl=zh-CN&gl=CA&ceid=CA:zh-Hans`
2. 媒体站点自带的 `/rss` 或 `/feed`
3. 论坛/社区的 `.rss` 地址

不建议一上来就做任意网页全文爬虫：容易触站规则、反爬和版权问题。若需要特定站点，优先找对方开放接口或授权 feed。

## 目录

```
topic-radar/
  server/     # Express API + RSS 采集
  src/        # React 界面
  data/       # 本地 JSON 存储（运行后生成）
```
