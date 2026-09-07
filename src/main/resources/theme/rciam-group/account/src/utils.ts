export function normalizeAccountConsoleUrl() {
  const accountPathMatch = window.location.pathname.match(
    /^(.*\/realms\/[^/]+\/account)(?:\/.*)?$/,
  );

  if (!accountPathMatch) {
    return;
  }

  const canonicalPath = `${accountPathMatch[1]}/`;
  const canonicalHash = window.location.hash || "#/";

  if (
    window.location.pathname !== canonicalPath ||
    window.location.hash !== canonicalHash
  ) {
    window.history.replaceState(
      window.history.state,
      "",
      `${canonicalPath}${window.location.search}${canonicalHash}`,
    );
  }
}