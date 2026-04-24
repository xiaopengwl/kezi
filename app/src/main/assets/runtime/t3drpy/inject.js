const currentRuleKey = () => globalThis.__drpyRuleKey || "default";

const requestRaw = (url, options = {}) =>
  AndroidBridge.request(
    JSON.stringify({
      url,
      options,
    }),
  );

const batchFetch = (requestList = []) => {
  if (!Array.isArray(requestList) || requestList.length === 0) {
    return [];
  }
  try {
    return JSON.parse(AndroidBridge.batchRequest(JSON.stringify(requestList)));
  } catch (error) {
    console.warn("batchFetch parse failed", error);
    return [];
  }
};

const req = (url, cobj = {}) => {
  const options = { ...cobj };
  if (options.data && !options.body) {
    options.body = options.data;
    delete options.data;
  }
  const response = requestRaw(url, options);
  if (options.withHeaders) {
    try {
      const parsed = JSON.parse(response);
      return {
        content: parsed.body || "",
        headers: parsed.headers || {},
      };
    } catch (error) {
      console.warn("req withHeaders parse failed", error);
      return {
        content: "",
        headers: {},
      };
    }
  }
  return {
    content: response || "",
  };
};

const joinUrl = (base, relative) => AndroidBridge.joinUrl(base || "", relative || "");

const local = {
  get(ruleKey, key, value = "") {
    return AndroidBridge.localGet(ruleKey || currentRuleKey(), key || "", String(value ?? ""));
  },
  set(ruleKey, key, value) {
    return AndroidBridge.localSet(ruleKey || currentRuleKey(), key || "", String(value ?? ""));
  },
  delete(ruleKey, key) {
    return AndroidBridge.localDelete(ruleKey || currentRuleKey(), key || "");
  },
};

const pd = (html, parse, baseUrl = globalThis.MY_URL || "") =>
  AndroidBridge.pd(String(html || ""), String(parse || ""), String(baseUrl || ""));

const pdfa = (html, parse) => {
  try {
    return JSON.parse(AndroidBridge.pdfa(String(html || ""), String(parse || "")));
  } catch (error) {
    console.warn("pdfa parse failed", error);
    return [];
  }
};

const pdfh = (html, parse, baseUrl = globalThis.MY_URL || "") =>
  AndroidBridge.pdfh(String(html || ""), String(parse || ""), String(baseUrl || ""));

const pdfl = (html, parse, listText, listUrl, urlKey = "") => {
  try {
    return JSON.parse(
      AndroidBridge.pdfl(
        String(html || ""),
        String(parse || ""),
        String(listText || ""),
        String(listUrl || ""),
        String(urlKey || ""),
      ),
    );
  } catch (error) {
    console.warn("pdfl parse failed", error);
    return [];
  }
};

export { batchFetch, joinUrl, local, pd, pdfa, pdfh, pdfl, req };
