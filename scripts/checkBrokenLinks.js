const fs = require('fs');
const path = require('path');
const axios = require('axios');

/**
 * Configuration for the broken link checker.
 */
const GITHUB_BASE = 'https://github.com/hiero-ledger/hiero-sdk-java/blob/main';

// Regex to identify private IP ranges and localhost to prevent SSRF-like behavior in the script
const PRIVATE_IP_RANGES = [
    /^127\./,
    /^10\./,
    /^172\.(1[6-9]|2[0-9]|3[0-1])\./,
    /^192\.168\./,
    /^localhost$/i
];

/**
 * Validates a single URL by sending a HEAD request.
 * Includes retry logic and security checks.
 */
async function checkLink(url, retries = 3) {
    // Only allow http and https
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
        return { broken: true, reason: 'Invalid protocol (only http/https allowed)' };
    }

    try {
        const parsedUrl = new URL(url);
        const hostname = parsedUrl.hostname;
        
        // Security check: Prevent access to internal network
        if (PRIVATE_IP_RANGES.some(range => range.test(hostname))) {
            return { broken: true, reason: 'Security Block: Access to private/local range denied' };
        }
    } catch (e) {
        return { broken: true, reason: 'Malformed URL' };
    }

    // Retry loop
    for (let i = 0; i < retries; i++) {
        try {
            // Using HEAD request for efficiency
            const response = await axios.head(url, { 
                timeout: 10000,
                headers: { 'User-Agent': 'Hiero-SDK-Broken-Link-Checker' }
            });
            
            if (response.status < 400) {
                return { broken: false };
            }
        } catch (error) {
            // If it's the last retry, record the failure
            if (i === retries - 1) {
                return { 
                    broken: true, 
                    status: error.response ? error.response.status : 'N/A',
                    reason: error.message 
                };
            }
            // Exponential-ish backoff: wait 1s, 2s...
            await new Promise(resolve => setTimeout(resolve, (i + 1) * 1000));
        }
    }
}

/**
 * Recursively scans a directory for Markdown files.
 */
async function scanFiles(dir, files = []) {
    const items = fs.readdirSync(dir);
    for (const item of items) {
        const fullPath = path.join(dir, item);
        const stat = fs.statSync(fullPath);
        
        if (stat.isDirectory()) {
            // Skip common non-source directories
            if (item !== 'node_modules' && item !== '.git' && item !== '.gradle' && item !== 'build') {
                await scanFiles(fullPath, files);
            }
        } else if (item.endsWith('.md')) {
            files.push(fullPath);
        }
    }
    return files;
}

/**
 * Main execution logic.
 */
async function main() {
    const rootDir = path.resolve(__dirname, '..');
    console.log(`\n🔍 Hiero SDK - Broken Link Checker`);
    console.log(`Scanning directory: ${rootDir}\n`);
    
    const mdFiles = await scanFiles(rootDir);
    console.log(`[1/3] Found ${mdFiles.length} markdown files.`);

    const linkRegex = /\[.*?\]\((.*?)\)/g;
    const allLinks = [];

    // Extract links from each file
    for (const file of mdFiles) {
        const content = fs.readFileSync(file, 'utf-8');
        const relativeFilePath = path.relative(rootDir, file).replace(/\\/g, '/');
        
        let match;
        while ((match = linkRegex.exec(content)) !== null) {
            let target = match[1].trim();

            // Ignore mailto links and internal anchors
            if (target.startsWith('mailto:') || target.startsWith('#')) {
                continue;
            }

            let finalUrl = target;
            if (!target.startsWith('http')) {
                // Convert relative links to GitHub blob URLs
                // We resolve the path relative to the file's directory
                const fileDir = path.dirname(relativeFilePath);
                const resolvedPath = path.posix.join(fileDir, target);
                finalUrl = `${GITHUB_BASE}/${resolvedPath}`;
            }

            allLinks.push({
                file: relativeFilePath,
                url: finalUrl,
                original: target
            });
        }
    }

    console.log(`[2/3] Extracted ${allLinks.length} unique links.`);
    console.log(`[3/3] Validating links... (Retries enabled, this might take a moment)\n`);

    const brokenLinks = [];
    let checkedCount = 0;

    for (const link of allLinks) {
        checkedCount++;
        // Print progress
        process.stdout.write(`   Progress: ${Math.round((checkedCount / allLinks.length) * 100)}% (${checkedCount}/${allLinks.length})\r`);
        
        const result = await checkLink(link.url);
        if (result.broken) {
            brokenLinks.push({ ...link, ...result });
        }
    }

    // Final Report Output
    console.log('\n\n' + '='.repeat(50));
    console.log('              AUDIT REPORT');
    console.log('='.repeat(50));

    if (brokenLinks.length === 0) {
        console.log('\n✅ Success: No broken links detected! All documentation is healthy.');
    } else {
        console.log(`\n❌ Found ${brokenLinks.length} broken links:\n`);
        brokenLinks.forEach((link, idx) => {
            console.log(`${idx + 1}. BROKEN LINK FOUND`);
            console.log(`   Source File : ${link.file}`);
            console.log(`   Original    : ${link.original}`);
            console.log(`   Resolved URL: ${link.url}`);
            console.log(`   Error       : ${link.reason} (Status: ${link.status || 'N/A'})\n`);
        });
    }

    console.log('='.repeat(50));
    console.log('                SUMMARY');
    console.log('='.repeat(50));
    console.log(`Total Files Scanned : ${mdFiles.length}`);
    console.log(`Total Links Checked : ${allLinks.length}`);
    console.log(`Broken Links Found  : ${brokenLinks.length}`);
    console.log('='.repeat(50) + '\n');
}

// Start the script
main().catch(err => {
    console.error('\nFatal Error during audit:', err.message);
    process.exit(1);
});
