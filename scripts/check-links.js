import 'dotenv/config';
import axios from 'axios';
import axiosRetry from 'axios-retry';
import { Octokit } from '@octokit/rest';
import path from 'path';

const REPO_OWNER = "hiero-ledger";
const REPO_NAME = "hiero-sdk-java";
const BRANCH = "main";

const octokit = new Octokit({ auth: process.env.GITHUB_TOKEN });
const retry = axiosRetry.default || axiosRetry;
retry(axios, { retries: 3, retryDelay: axiosRetry.exponentialDelay });

const brokenLinks = [];

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

function resolveRelativeLink(repoPath, relLink) {
  const baseDir = path.posix.dirname(repoPath);
  const normalizedPath = path.posix.normalize(
    path.posix.join(baseDir, relLink)
  );
  // Corrected template literal syntax below
  return `https://github.com/${REPO_OWNER}/${REPO_NAME}/blob/${BRANCH}/${normalizedPath}`;
}

async function checkLink(url) {
  try {
    const parsedUrl = new URL(url);
    const isPublicWeb = ['http:', 'https:'].includes(parsedUrl.protocol);
    const isInternal = parsedUrl.hostname === 'localhost' || 
                       parsedUrl.hostname.startsWith('127.') || 
                       parsedUrl.hostname.startsWith('192.168.');

    if (!isPublicWeb || isInternal) {
      return; 
    }

    const res = await axios.head(url, {
      timeout: 20000,
      validateStatus: () => true,
    });
    
    if (res.status >= 400) {
      const reason = STATUS_DESCRIPTIONS[res.status] || "Unknown Error";
      console.log(`[BROKEN] ${url} - ${res.status} ${reason}`);
      brokenLinks.push({ url, status: res.status, reason });
    }
  } catch (err) {
    if (err.code !== 'ERR_INVALID_URL') {
      console.log(`[ERROR] ${url} - Request failed: ${err.message}`);
      brokenLinks.push({ url, status: "Request failed", reason: err.message });
    }
  }
}

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