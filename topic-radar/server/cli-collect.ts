import { collectAllTopics } from './collector.js'

const results = await collectAllTopics()
for (const result of results) {
  console.log(
    `${result.topicName}: fetched=${result.fetched} added=${result.added} updated=${result.updated} errors=${result.sourceErrors.length}`,
  )
  for (const error of result.sourceErrors) {
    console.log(`  ! ${error.name}: ${error.error}`)
  }
}
