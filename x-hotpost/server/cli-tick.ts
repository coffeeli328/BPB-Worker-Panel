import { runTick } from './worker.js'

const result = await runTick('cli')
console.log(JSON.stringify(result, null, 2))
