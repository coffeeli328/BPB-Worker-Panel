import { createHmac, randomBytes } from 'node:crypto'

type OAuthParams = Record<string, string>

function percentEncode(value: string): string {
  return encodeURIComponent(value).replace(/[!'()*]/g, (c) =>
    `%${c.charCodeAt(0).toString(16).toUpperCase()}`,
  )
}

function normalizeParams(params: OAuthParams): string {
  return Object.keys(params)
    .sort()
    .map((key) => `${percentEncode(key)}=${percentEncode(params[key]!)}`)
    .join('&')
}

export function buildOAuth1Header(options: {
  method: string
  url: string
  consumerKey: string
  consumerSecret: string
  token: string
  tokenSecret: string
}): string {
  const oauth: OAuthParams = {
    oauth_consumer_key: options.consumerKey,
    oauth_nonce: randomBytes(16).toString('hex'),
    oauth_signature_method: 'HMAC-SHA1',
    oauth_timestamp: Math.floor(Date.now() / 1000).toString(),
    oauth_token: options.token,
    oauth_version: '1.0',
  }

  const baseString = [
    options.method.toUpperCase(),
    percentEncode(options.url),
    percentEncode(normalizeParams(oauth)),
  ].join('&')

  const signingKey = `${percentEncode(options.consumerSecret)}&${percentEncode(options.tokenSecret)}`
  const signature = createHmac('sha1', signingKey)
    .update(baseString)
    .digest('base64')

  oauth.oauth_signature = signature

  const header = Object.keys(oauth)
    .sort()
    .map((key) => `${percentEncode(key)}="${percentEncode(oauth[key]!)}"`)
    .join(', ')

  return `OAuth ${header}`
}
