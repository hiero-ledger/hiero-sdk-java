import 'dotenv/config';
import axios from 'axios';
import axiosRetry from 'axios-retry';
import { Octokit } from '@octokit/rest';
import path from 'path';

const REPO_OWNER = "hiero-ledger";
const REPO_NAME = "hiero-sdk-java";
const BRANCH = "main";

const octokit = new Octokit({ auth: process.env.GITHUB_TOKEN });
// Handle axiosRetry which sometimes exports differently in ESM
const retry = axiosRetry.default || axiosRetry;
retry(axios, { retries: 3, retryDelay: axiosRetry.exponentialDelay });

const brokenLinks = [];

// Status code → description map
const STATUS_DESCRIPTIONS = {
  400: "Bad Request",
  401: "Unauthorized",
  403: "Forbidden",
  404: "Not Found",
  405: "Method Not Allowed",
  408: "Request Timeout",
  429: "Too Many Requests",
  500: "Internal Server Error",
  502: "Bad Gateway",
  503: "Service Unavailable",
  504: "Gateway Timeout",
};

// Get all .md files in the repo recursively
async function getMarkdownFiles(currentPath = "") {
  const response = await octokit.repos.getContent({
    owner: REPO_OWNER,
    repo: REPO_NAME,
    path: currentPath,
  });

  const files = [];

  for (const item of response.data) {
    if (item.type === "file" && item.name.endsWith(".md")) {
      files.push({ download_url: item.download_url, repo_path: item.path });
    } else if (item.type === "dir") {
      const nestedFiles = await getMarkdownFiles(item.path);
      files.push(...nestedFiles);
    }
  }

  return files;
}

// Extract markdown links
function extractLinks(markdown) {
  const regex = /\[.*?\]\((.*?)\)/g;
  const links = new Set();
  let match;
  while ((match = regex.exec(markdown)) !== null) {
    const link = match[1];
    if (!link.startsWith("mailto:")) {
      links.add(link);
    }
  }
  return [...links];
}

// Resolve relative links to GitHub blob URL
function resolveRelativeLink(repoPath, relLink) {
  const baseDir = path.posix.dirname(repoPath);
  const normalizedPath = path.posix.normalize(
    path.posix.join(baseDir, relLink)
  );
  return `https://github.com/${REPO_OWNER}/${REPO_NAME}/blob/${BRANCH}/${normalizedPath}`;
}

// Check if a link works
async function checkLink(url) {
  let parsedUrl;
  try {
    parsedUrl = new URL(url);
  } catch {
    return;
  }

  // 1. Only allow standard web protocols
  if (!['http:', 'https:'].includes(parsedUrl.protocol)) {
    return;
  }

  // 2. SSRF PROTECTION: Explicitly block internal/private IP ranges
  const hostname = parsedUrl.hostname.toLowerCase();
  const forbiddenHostnames = ['localhost', '127.0.0.1', '0.0.0.0'];

  // Check for common private IP patterns and Cloud Metadata (169.254.x)
  const isPrivateIP = /^(10\.|192\.168\.|172\.(1[6-9]|2[0-9]|3[0-1])\.|169\.254\.)/.test(hostname);

  if (forbiddenHostnames.includes(hostname) || isPrivateIP) {
    return;
  }

  // 3. Reconstruct URL from validated components only
  // This breaks the taint chain by building a new URL from trusted parts
  const safeUrl = new URL('about:blank');
  safeUrl.protocol = parsedUrl.protocol;
  safeUrl.hostname = parsedUrl.hostname;
  safeUrl.port     = parsedUrl.port;
  safeUrl.pathname = parsedUrl.pathname;
  safeUrl.search   = parsedUrl.search;
  const finalUrl   = safeUrl.toString();

  try {
    const res = await axios.head(finalUrl, {
      timeout: 20000,
      validateStatus: () => true,
      headers: { 'User-Agent': 'Mozilla/5.0 (Broken-Link-Checker)' }
    });

    if (res.status >= 400) {
      const reason = STATUS_DESCRIPTIONS[res.status] || "Unknown Error";
      console.log(`[BROKEN] ${finalUrl} - ${res.status} ${reason}`);
      brokenLinks.push({ url: finalUrl, status: res.status, reason });
    }
  } catch (err) {
    console.log(`[ERROR] ${finalUrl} - Request failed: ${err.message}`);
    brokenLinks.push({ url: finalUrl, status: "Request failed", reason: err.message });
  }
}

// Main
(async () => {
  console.log(`🔍 Crawling markdown files in ${REPO_OWNER}/${REPO_NAME}...\n`);

  let mdFiles = [];
  try {
    mdFiles = await getMarkdownFiles();
  } catch (err) {
    console.error("❌ Failed to fetch markdown files:", err.message);
    process.exit(1);
  }

  const allLinks = new Set();

  for (const { download_url, repo_path } of mdFiles) {
    try {
      const res = await axios.get(download_url, { timeout: 15000 });
      const links = extractLinks(res.data);
      for (const link of links) {
        if (link.startsWith("http")) {
          allLinks.add(link);
        } else if (!link.startsWith("#")) {
          allLinks.add(resolveRelativeLink(repo_path, link));
        }
      }
    } catch (err) {
      console.error(`[FAILED TO LOAD MD] ${download_url}`);
    }
  }

  console.log(`\n🔗 Found ${allLinks.size} unique links. Checking...\n`);

  for (const link of allLinks) {
    await checkLink(link);
  }

  console.log(`\n✅ Done. ${brokenLinks.length} broken links found.`);
})();