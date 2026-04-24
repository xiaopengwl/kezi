import drpy from "./t3drpy/drpy2.min.js";

function safeString(value) {
  return value === undefined || value === null ? "" : String(value);
}

function maybeJson(value) {
  if (typeof value !== "string") {
    return value ?? {};
  }
  try {
    return JSON.parse(value);
  } catch (_) {
    return value;
  }
}

function requestText(url, options = {}) {
  return AndroidBridge.request(
    JSON.stringify({
      url,
      options,
    }),
  );
}

function isUrl(value) {
  return /^(https?:)?\/\//i.test(safeString(value));
}

function ensureAbsolute(base, value) {
  const url = safeString(value);
  if (!url) return "";
  if (/^(https?:|magnet:|thunder:|ws:|ftp:)/i.test(url)) {
    return url;
  }
  return AndroidBridge.joinUrl(safeString(base), url);
}

function parseLegacyRule(source) {
  let rule = null;
  try {
    eval(source + "\n//# sourceURL=legacy_rule.js");
  } catch (error) {
    throw new Error("规则解析失败: " + error.message);
  }
  if (!rule) {
    throw new Error("未找到 rule 对象");
  }
  return rule;
}

function parseLegacyClasses(rule) {
  const names = safeString(rule.class_name).split("&").filter(Boolean);
  const urls = safeString(rule.class_url).split("&").filter(Boolean);
  const list = [];
  for (let i = 0; i < Math.min(names.length, urls.length); i += 1) {
    list.push({
      name: names[i],
      typeId: urls[i],
    });
  }
  return list;
}

function buildLegacyCategoryUrl(rule, typeId, page) {
  return safeString(rule.url)
    .replace(/fyclass/g, typeId)
    .replace(/fypage/g, String(page || 1))
    .replace(/fyfilter/g, "");
}

function buildLegacySearchUrl(rule, keyword, page) {
  const template = safeString(rule.searchUrl || rule.search_url || "");
  return template
    .replace(/\*\*/g, encodeURIComponent(keyword || ""))
    .replace(/fypage/g, String(page || 1));
}

function normalizeLegacyList(result, rule) {
  if (!Array.isArray(result)) {
    return [];
  }
  return result
    .map(function (item) {
      return {
        title: safeString(item.title || item.vod_name),
        img: safeString(item.img || item.vod_pic),
        desc: safeString(item.desc || item.vod_remarks || item.vod_desc),
        url: ensureAbsolute(rule.host || "", item.url || item.vod_id),
      };
    })
    .filter(function (item) {
      return item.title && item.url;
    });
}

function normalizePlayGroups(detail) {
  const playFrom = safeString(detail.vod_play_from).split("$$$").filter(Boolean);
  const playUrls = safeString(detail.vod_play_url).split("$$$").filter(Boolean);
  const groups = [];
  for (let i = 0; i < Math.min(playFrom.length, playUrls.length); i += 1) {
    const name = playFrom[i] || "线路" + (i + 1);
    const episodes = playUrls[i]
      .split("#")
      .filter(Boolean)
      .map(function (episode) {
        const parts = episode.split("$");
        return {
          name: safeString(parts[0] || "正片"),
          url: safeString(parts.slice(1).join("$")),
        };
      })
      .filter(function (episode) {
        return episode.url;
      });
    if (episodes.length > 0) {
      groups.push({
        name: name,
        episodes: episodes,
      });
    }
  }
  return groups;
}

function normalizeLegacyDetail(detail) {
  detail = detail || {};
  return {
    title: safeString(detail.vod_name),
    img: safeString(detail.vod_pic),
    remarks: safeString(detail.vod_remarks),
    year: safeString(detail.vod_year),
    area: safeString(detail.vod_area),
    director: safeString(detail.vod_director),
    actor: safeString(detail.vod_actor),
    content: safeString(detail.vod_content),
    playGroups: normalizePlayGroups(detail),
  };
}

function normalizeLegacyPlay(play) {
  if (typeof play === "string") {
    return {
      url: play,
      parse: false,
      jx: 0,
      headers: {},
    };
  }
  play = play || {};
  return {
    url: safeString(play.url || play.input || ""),
    parse: !!play.parse,
    jx: Number(play.jx || 0),
    headers: play.header || play.headers || {},
  };
}

function resetLegacyEnv(rule, input, page) {
  window.rule = rule;
  window.input = input;
  window.MY_URL = input;
  window.MY_PAGE = page || 1;
  window.MY_PAGECOUNT = 999;
  window.MY_TOTAL = 9999;
  window.MY_FL = {};
  window.VOD = null;
  window.__drpyResult = null;
}

function setupLegacyHelpers() {
  window.setResult = function (value) {
    window.__drpyResult = value;
    return value;
  };
  window.request = function (url, options) {
    return requestText(url, options || {});
  };
  window.post = function (url, body, options) {
    const merged = options || {};
    merged.method = "POST";
    merged.body = body || "";
    return requestText(url, merged);
  };
  window.urljoin = function (base, relative) {
    return AndroidBridge.joinUrl(safeString(base), safeString(relative));
  };
  window.base64Encode = function (text) {
    return AndroidBridge.base64Encode(safeString(text));
  };
  window.base64Decode = function (text) {
    return AndroidBridge.base64Decode(safeString(text));
  };
  window.log = function () {
    AndroidBridge.log(Array.prototype.slice.call(arguments).join(" "));
  };
}

function runLegacyBlock(block, rule, input, page) {
  resetLegacyEnv(rule, input, page);
  setupLegacyHelpers();

  if (typeof block === "function") {
    block();
  } else if (typeof block === "string") {
    const code = block.indexOf("js:") === 0 ? block.substring(3) : block;
    eval(code);
  } else {
    return block;
  }

  if (window.__drpyResult !== null && window.__drpyResult !== undefined) {
    return window.__drpyResult;
  }
  if (window.VOD) {
    return window.VOD;
  }
  return null;
}

function executeLegacy(sourceText, action, input, page) {
  const rule = parseLegacyRule(sourceText);
  const data = {};

  if (action === "home") {
    data.meta = {
      title: safeString(rule.title || rule.host),
      host: safeString(rule.host),
      searchable: !!rule.searchable,
      categories: parseLegacyClasses(rule),
    };
    data.list = normalizeLegacyList(runLegacyBlock(rule["推荐"], rule, rule.host, 1), rule);
  } else if (action === "category") {
    data.list = normalizeLegacyList(
      runLegacyBlock(rule["一级"], rule, buildLegacyCategoryUrl(rule, input, page), page),
      rule,
    );
  } else if (action === "search") {
    data.list = normalizeLegacyList(
      runLegacyBlock(rule["搜索"], rule, buildLegacySearchUrl(rule, input, page), page),
      rule,
    );
  } else if (action === "detail") {
    data.detail = normalizeLegacyDetail(runLegacyBlock(rule["二级"], rule, ensureAbsolute(rule, input), 1));
  } else if (action === "lazy") {
    data.play = normalizeLegacyPlay(runLegacyBlock(rule["lazy"], rule, input, 1));
  } else {
    throw new Error("不支持的动作: " + action);
  }

  return data;
}

function parseZyfunPackage(sourceText) {
  const trimmed = safeString(sourceText).trim();
  if (!trimmed || (!trimmed.startsWith("{") && !trimmed.startsWith("["))) {
    return null;
  }
  const parsed = JSON.parse(trimmed);
  const siteList = Array.isArray(parsed)
    ? parsed
    : Array.isArray(parsed.site)
      ? parsed.site
      : Array.isArray(parsed.sites)
        ? parsed.sites
        : [];
  if (!siteList.length) {
    return null;
  }
  const settingList = Array.isArray(parsed.setting)
    ? parsed.setting
    : parsed.setting && typeof parsed.setting === "object"
      ? [parsed.setting]
      : [];
  const defaultSite = safeString(settingList[0] && settingList[0].defaultSite);
  const activeSites = siteList.filter((site) => site && site.isActive !== false);
  const preferredSite =
    activeSites.find((site) => safeString(site.id) === defaultSite || safeString(site.key) === defaultSite) ||
    activeSites.find((site) => Number(site.type) === 7) ||
    activeSites[0];
  if (!preferredSite) {
    return null;
  }
  return {
    raw: parsed,
    site: preferredSite,
  };
}

function isSearchEnabled(value) {
  if (value === false) return false;
  if (value === 0 || value === "0") return false;
  return true;
}

const drpyState = {
  extUrl: "",
  extText: "",
  initializedNamespace: "",
  initializedExtText: "",
};

function normalizeCmsList(list) {
  if (!Array.isArray(list)) {
    return [];
  }
  return list
    .map((item) => ({
      title: safeString(item.vod_name || item.title || item.name),
      img: safeString(item.vod_pic || item.img || item.pic),
      desc: safeString(item.vod_remarks || item.vod_blurb || item.desc),
      url: safeString(item.vod_id || item.url || item.id),
    }))
    .filter((item) => item.title && item.url);
}

function normalizeCmsDetail(vod) {
  vod = vod || {};
  return {
    title: safeString(vod.vod_name),
    img: safeString(vod.vod_pic),
    remarks: safeString(vod.vod_remarks),
    year: safeString(vod.vod_year),
    area: safeString(vod.vod_area),
    director: safeString(vod.vod_director),
    actor: safeString(vod.vod_actor),
    content: safeString(vod.vod_content || vod.vod_blurb),
    playGroups: normalizePlayGroups(vod),
  };
}

function normalizeCmsPlay(play) {
  play = maybeJson(play) || {};
  const url =
    Array.isArray(play.url) && play.url.length > 1
      ? safeString(play.url[1])
      : safeString(play.url);
  return {
    url,
    parse: Number(play.parse || 0) > 0,
    jx: Number(play.jx || 0),
    headers: play.header || play.headers || {},
  };
}

function ensureDrpyExt(site) {
  const rawExt = site && site.ext;
  if (typeof rawExt === "string" && isUrl(rawExt)) {
    if (drpyState.extUrl === safeString(rawExt) && drpyState.extText) {
      return drpyState.extText;
    }
    const text = requestText(rawExt, {});
    drpyState.extUrl = safeString(rawExt);
    drpyState.extText = text;
    return text;
  }
  drpyState.extUrl = "";
  return safeString(rawExt);
}

function ensureDrpyInit(site) {
  const extText = ensureDrpyExt(site);
  const namespace = safeString(site.key || site.id || site.name || "default");
  globalThis.__drpyRuleKey = namespace;
  if (drpyState.initializedNamespace === namespace && drpyState.initializedExtText === extText) {
    return;
  }
  drpy.init(extText);
  drpyState.initializedNamespace = namespace;
  drpyState.initializedExtText = extText;
}

function currentDrpyRule() {
  const rule = maybeJson(drpy.getRule ? drpy.getRule() : {});
  return typeof rule === "object" && rule ? rule : {};
}

function executeT3Drpy(sourcePackage, action, input, page, flag) {
  const site = sourcePackage.site;
  if (Number(site.type) !== 7) {
    throw new Error("当前仅支持 zyfun 的 T3_DRPY(type=7) 站源");
  }
  ensureDrpyInit(site);
  const rule = currentDrpyRule();
  const data = {};

  if (action === "home") {
    const home = maybeJson(drpy.home()) || {};
    const homeVod = maybeJson(drpy.homeVod()) || {};
    data.meta = {
      title: safeString(site.name || rule.title || rule.host),
      host: safeString(rule.host || site.api),
      searchable: isSearchEnabled(site.search),
      categories: Array.isArray(home.class)
        ? home.class
            .map((item) => ({
              name: safeString(item.type_name),
              typeId: safeString(item.type_id),
            }))
            .filter((item) => item.name && item.typeId)
        : [],
    };
    data.list = normalizeCmsList(homeVod.list || []);
  } else if (action === "category") {
    const category = maybeJson(drpy.category(input, page, false, {})) || {};
    data.list = normalizeCmsList(category.list || []);
  } else if (action === "search") {
    const result = maybeJson(drpy.search(input, false, page)) || {};
    data.list = normalizeCmsList(result.list || []);
  } else if (action === "detail") {
    const detail = maybeJson(drpy.detail(input)) || {};
    data.detail = normalizeCmsDetail((detail.list || [])[0] || {});
  } else if (action === "lazy") {
    const play = maybeJson(drpy.play(flag || "", input, [])) || {};
    data.play = normalizeCmsPlay(play);
  } else {
    throw new Error("不支持的动作: " + action);
  }

  return data;
}

window.__nativeRuleRuntime = {
  execute(payloadJson) {
    try {
      const payload = JSON.parse(payloadJson);
      const action = safeString(payload.action);
      const input = safeString(payload.input);
      const page = Number(payload.page || 1);
      const flag = safeString(payload.flag);
      const sourcePackage = parseZyfunPackage(payload.source);
      const data = sourcePackage
        ? executeT3Drpy(sourcePackage, action, input, page, flag)
        : executeLegacy(payload.source, action, input, page);

      return JSON.stringify({
        ok: true,
        data,
      });
    } catch (error) {
      return JSON.stringify({
        ok: false,
        error: error && error.message ? error.message : String(error),
      });
    }
  },
};
