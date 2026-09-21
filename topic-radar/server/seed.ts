import { v4 as uuid } from 'uuid'
import type { StoreData } from './types.js'

export function createSeedStore(): StoreData {
  const now = new Date().toISOString()
  const topicId = uuid()

  return {
    topics: [
      {
        id: topicId,
        name: '温哥华房价',
        description: '聚合温哥华及大温地区房价、成交量、新盘与政策相关报道。',
        keywords: [
          '温哥华',
          '大温',
          '房价',
          '楼市',
          '房产',
          '按揭',
          'condo',
          'housing',
          'real estate',
          'home sales',
          'home prices',
          'mortgage',
          'vancouver',
          'metro vancouver',
          'greater vancouver',
          'benchmark',
          'listing',
        ],
        sources: [
          {
            id: uuid(),
            name: 'Google News · Vancouver housing',
            url: 'https://news.google.com/rss/search?q=Vancouver+housing+prices&hl=en-CA&gl=CA&ceid=CA:en',
            type: 'rss',
            enabled: true,
          },
          {
            id: uuid(),
            name: 'Google News · 温哥华房价',
            url: 'https://news.google.com/rss/search?q=%E6%B8%A9%E5%93%A5%E5%8D%8E+%E6%88%BF%E4%BB%B7&hl=zh-CN&gl=CA&ceid=CA:zh-Hans',
            type: 'rss',
            enabled: true,
          },
          {
            id: uuid(),
            name: 'CBC British Columbia',
            url: 'https://www.cbc.ca/webfeed/rss/rss-canada-britishcolumbia',
            type: 'rss',
            enabled: true,
          },
          {
            id: uuid(),
            name: 'Reddit · r/vancouverhousing',
            url: 'https://www.reddit.com/r/vancouverhousing/.rss',
            type: 'rss',
            enabled: true,
          },
        ],
        createdAt: now,
        updatedAt: now,
      },
    ],
    articles: [],
  }
}
